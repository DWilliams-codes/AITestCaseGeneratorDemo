package com.testforge.requirement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.application.AuditMetadata;
import com.testforge.audit.application.AuditService;
import com.testforge.common.dto.PageResponse;
import com.testforge.common.error.ApiExceptions;
import com.testforge.common.workitem.WorkItemNumberService;
import com.testforge.project.application.ProjectService;
import com.testforge.requirement.domain.AcceptanceCriterionEntity;
import com.testforge.requirement.domain.RequirementAmbiguityEntity;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.requirement.domain.RequirementRevisionEntity;
import com.testforge.requirement.domain.RequirementStatus;
import com.testforge.requirement.dto.RequirementDtos.AcceptanceCriterionRequest;
import com.testforge.requirement.dto.RequirementDtos.AcceptanceCriterionResponse;
import com.testforge.requirement.dto.RequirementDtos.AmbiguityResponse;
import com.testforge.requirement.dto.RequirementDtos.CreateRequirementRequest;
import com.testforge.requirement.dto.RequirementDtos.RequirementResponse;
import com.testforge.requirement.dto.RequirementDtos.RequirementSummaryResponse;
import com.testforge.requirement.dto.RequirementDtos.ResolveAmbiguityRequest;
import com.testforge.requirement.dto.RequirementDtos.UpdateRequirementRequest;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.requirement.repository.RequirementAmbiguityRepository;
import com.testforge.requirement.repository.RequirementRepository;
import com.testforge.requirement.repository.RequirementRevisionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementService {
  private final RequirementRepository requirements;
  private final WorkItemNumberService workItemNumbers;
  private final AcceptanceCriterionRepository criteria;
  private final RequirementAmbiguityRepository ambiguities;
  private final RequirementRevisionRepository revisions;
  private final ProjectService projectService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Initializes RequirementService with its required collaborators and domain state. */
  public RequirementService(
      RequirementRepository requirements,
      WorkItemNumberService workItemNumbers,
      AcceptanceCriterionRepository criteria,
      RequirementAmbiguityRepository ambiguities,
      RequirementRevisionRepository revisions,
      ProjectService projectService,
      AuditService auditService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.requirements = requirements;
    this.workItemNumbers = workItemNumbers;
    this.criteria = criteria;
    this.ambiguities = ambiguities;
    this.revisions = revisions;
    this.projectService = projectService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Lists resources visible to the current owner using the requested page. */
  @Transactional(readOnly = true)
  public PageResponse<RequirementSummaryResponse> list(
      UUID ownerId, UUID projectId, int page, int size) {
    projectService.requireOwned(ownerId, projectId);
    var requirementPage =
        requirements.findAllByProjectIdOrderByUpdatedAtDesc(projectId, PageRequest.of(page, size));
    Map<UUID, Long> counts =
        requirementPage.isEmpty()
            ? Map.of()
            : criteria
                .countByRequirementIds(
                    requirementPage.getContent().stream().map(RequirementEntity::getId).toList())
                .stream()
                .collect(
                    Collectors.toMap(
                        item -> item.getRequirementId(),
                        item -> item.getCriterionCount(),
                        (left, right) -> left));
    return PageResponse.from(
        requirementPage.map(
            requirement -> toSummary(requirement, counts.getOrDefault(requirement.getId(), 0L))));
  }

  /** Returns the owned resource identified by the request. */
  @Transactional(readOnly = true)
  public RequirementResponse get(UUID ownerId, UUID requirementId) {
    return toResponse(requireOwned(ownerId, requirementId));
  }

  /** Creates and persists a new domain resource from validated input. */
  @Transactional
  public RequirementResponse create(
      UUID ownerId, UUID projectId, CreateRequirementRequest request) {
    projectService.requireOwned(ownerId, projectId);
    Instant now = clock.instant();
    RequirementEntity requirement =
        requirements.save(
            RequirementEntity.create(
                workItemNumbers.next(),
                projectId,
                request.title().strip(),
                request.userStory().strip(),
                clean(request.businessRequirements()),
                clean(request.assumptions()),
                clean(request.sourceReference()),
                request.priority(),
                ownerId,
                now));
    int order = 0;
    for (String description : request.acceptanceCriteria()) {
      criteria.save(
          AcceptanceCriterionEntity.create(
              requirement.getId(), "AC-" + (order + 1), description.strip(), order, now));
      order++;
    }
    auditService.record(
        ownerId, projectId, "REQUIREMENT", requirement.getId(), "CREATED", AuditMetadata.empty());
    return toResponse(requirement);
  }

  /** Applies a validated update while preserving concurrency guarantees. */
  @Transactional
  public RequirementResponse update(
      UUID ownerId, UUID requirementId, UpdateRequirementRequest request) {
    RequirementEntity requirement = requireOwned(ownerId, requirementId);
    assertVersion(requirement.getVersion(), request.version(), "requirement");
    assertUserManagedStatus(request.status());
    saveRevision(requirement, ownerId);
    requirement.update(
        request.title().strip(),
        request.userStory().strip(),
        clean(request.businessRequirements()),
        clean(request.assumptions()),
        clean(request.sourceReference()),
        request.status(),
        request.priority(),
        clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "REQUIREMENT",
        requirementId,
        "UPDATED",
        AuditMetadata.requirementStatus(request.status().name()));
    return toResponse(requirement);
  }

  /** Executes the add criterion operation for RequirementService. */
  @Transactional
  public AcceptanceCriterionResponse addCriterion(
      UUID ownerId,
      UUID requirementId,
      long expectedRequirementVersion,
      AcceptanceCriterionRequest request) {
    RequirementEntity requirement = requireOwnedForUpdate(ownerId, requirementId);
    assertVersion(requirement.getVersion(), expectedRequirementVersion, "User Story");
    List<AcceptanceCriterionEntity> existing =
        criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    if (existing.size() >= 50) {
      throw ApiExceptions.badRequest(
          "criteria_limit_exceeded", "A requirement may have at most 50 acceptance criteria.");
    }
    ensureCriterionUnique(existing, null, request);
    saveRevision(requirement, ownerId, existing);
    AcceptanceCriterionEntity criterion =
        criteria.save(
            AcceptanceCriterionEntity.create(
                requirementId,
                normalizeKey(request.criterionKey()),
                request.description().strip(),
                request.sortOrder(),
                clock.instant()));
    requirement.markCriteriaChanged(clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "ACCEPTANCE_CRITERION",
        criterion.getId(),
        "CREATED",
        AuditMetadata.criterion(criterion.getCriterionKey()));
    return toCriterion(criterion);
  }

  /** Executes the update criterion operation for RequirementService. */
  @Transactional
  public AcceptanceCriterionResponse updateCriterion(
      UUID ownerId,
      UUID criterionId,
      long expectedRequirementVersion,
      AcceptanceCriterionRequest request) {
    UUID requirementId =
        criteria
            .findOwnedRequirementId(criterionId, ownerId)
            .orElseThrow(() -> ApiExceptions.notFound("Acceptance criterion not found."));
    RequirementEntity requirement = requireOwnedForUpdate(ownerId, requirementId);
    assertVersion(requirement.getVersion(), expectedRequirementVersion, "User Story");
    AcceptanceCriterionEntity criterion =
        criteria
            .findById(criterionId)
            .filter(item -> item.getRequirementId().equals(requirementId))
            .orElseThrow(() -> ApiExceptions.notFound("Acceptance criterion not found."));
    List<AcceptanceCriterionEntity> existing =
        criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    ensureCriterionUnique(existing, criterionId, request);
    saveRevision(requirement, ownerId, existing);
    criterion.update(
        normalizeKey(request.criterionKey()),
        request.description().strip(),
        request.sortOrder(),
        clock.instant());
    requirement.markCriteriaChanged(clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "ACCEPTANCE_CRITERION",
        criterionId,
        "UPDATED",
        AuditMetadata.criterion(criterion.getCriterionKey()));
    return toCriterion(criterion);
  }

  /** Deletes criterion from persistent storage. */
  @Transactional
  public void deleteCriterion(UUID ownerId, UUID criterionId, long expectedRequirementVersion) {
    UUID requirementId =
        criteria
            .findOwnedRequirementId(criterionId, ownerId)
            .orElseThrow(() -> ApiExceptions.notFound("Acceptance criterion not found."));
    RequirementEntity requirement = requireOwnedForUpdate(ownerId, requirementId);
    assertVersion(requirement.getVersion(), expectedRequirementVersion, "User Story");
    AcceptanceCriterionEntity criterion =
        criteria
            .findById(criterionId)
            .filter(item -> item.getRequirementId().equals(requirementId))
            .orElseThrow(() -> ApiExceptions.notFound("Acceptance criterion not found."));
    List<AcceptanceCriterionEntity> existing =
        criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    if (existing.size() <= 1) {
      throw ApiExceptions.badRequest(
          "minimum_criteria_required",
          "A requirement must keep at least one acceptance criterion.");
    }
    saveRevision(requirement, ownerId, existing);
    criteria.delete(criterion);
    requirement.markCriteriaChanged(clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "ACCEPTANCE_CRITERION",
        criterionId,
        "DELETED",
        AuditMetadata.criterion(criterion.getCriterionKey()));
  }

  /** Resolves ambiguity for the current operation. */
  @Transactional
  public AmbiguityResponse resolveAmbiguity(
      UUID ownerId, UUID ambiguityId, ResolveAmbiguityRequest request) {
    RequirementAmbiguityEntity ambiguity =
        ambiguities
            .findOwned(ambiguityId, ownerId)
            .orElseThrow(() -> ApiExceptions.notFound("Ambiguity not found."));
    assertVersion(ambiguity.getVersion(), request.version(), "ambiguity");
    RequirementEntity requirement = requireOwned(ownerId, ambiguity.getRequirementId());
    ambiguity.resolve(request.resolution().strip(), clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "REQUIREMENT_AMBIGUITY",
        ambiguityId,
        "RESOLVED",
        AuditMetadata.empty());
    return toAmbiguity(ambiguity);
  }

  /** Loads the requested resource and verifies that it belongs to the current owner. */
  @Transactional(readOnly = true)
  public RequirementEntity requireOwned(UUID ownerId, UUID requirementId) {
    return requirements
        .findOwned(requirementId, ownerId)
        .orElseThrow(() -> ApiExceptions.notFound("Requirement not found."));
  }

  /** Requires owned for update for the current operation. */
  private RequirementEntity requireOwnedForUpdate(UUID ownerId, UUID requirementId) {
    return requirements
        .findOwnedForUpdate(requirementId, ownerId)
        .orElseThrow(() -> ApiExceptions.notFound("Requirement not found."));
  }

  /** Maps the source data to response. */
  private RequirementResponse toResponse(RequirementEntity requirement) {
    return new RequirementResponse(
        requirement.getId(),
        requirement.getWorkItemNumber(),
        requirement.getProjectId(),
        requirement.getTitle(),
        requirement.getUserStory(),
        requirement.getBusinessRequirements(),
        requirement.getAssumptions(),
        requirement.getSourceReference(),
        requirement.getStatus(),
        requirement.getPriority(),
        criteria.findAllByRequirementIdOrderBySortOrder(requirement.getId()).stream()
            .map(this::toCriterion)
            .toList(),
        ambiguities.findAllByRequirementIdOrderByCreatedAt(requirement.getId()).stream()
            .map(this::toAmbiguity)
            .toList(),
        requirement.getCreatedAt(),
        requirement.getUpdatedAt(),
        requirement.getVersion());
  }

  /** Maps one list summary using a criterion count already loaded for the page. */
  private RequirementSummaryResponse toSummary(
      RequirementEntity requirement, long acceptanceCriteriaCount) {
    return new RequirementSummaryResponse(
        requirement.getId(),
        requirement.getWorkItemNumber(),
        requirement.getProjectId(),
        requirement.getTitle(),
        requirement.getStatus(),
        requirement.getPriority(),
        Math.toIntExact(acceptanceCriteriaCount),
        requirement.getUpdatedAt(),
        requirement.getVersion());
  }

  /** Maps the source data to criterion. */
  private AcceptanceCriterionResponse toCriterion(AcceptanceCriterionEntity criterion) {
    return new AcceptanceCriterionResponse(
        criterion.getId(),
        criterion.getCriterionKey(),
        criterion.getDescription(),
        criterion.getSortOrder(),
        criterion.getCreatedAt(),
        criterion.getUpdatedAt());
  }

  /** Maps the source data to ambiguity. */
  private AmbiguityResponse toAmbiguity(RequirementAmbiguityEntity ambiguity) {
    return new AmbiguityResponse(
        ambiguity.getId(),
        ambiguity.getCategory(),
        ambiguity.getSeverity(),
        ambiguity.getDescription(),
        ambiguity.getSuggestedQuestion(),
        ambiguity.isResolved(),
        ambiguity.getResolution(),
        ambiguity.getCreatedAt(),
        ambiguity.getResolvedAt(),
        ambiguity.getVersion());
  }

  /** Executes the ensure criterion unique operation for RequirementService. */
  private void ensureCriterionUnique(
      List<AcceptanceCriterionEntity> existing,
      UUID currentId,
      AcceptanceCriterionRequest request) {
    String key = normalizeKey(request.criterionKey());
    boolean duplicate =
        existing.stream()
            .filter(item -> !item.getId().equals(currentId))
            .anyMatch(
                item ->
                    item.getCriterionKey().equals(key)
                        || item.getSortOrder() == request.sortOrder());
    if (duplicate) {
      throw ApiExceptions.conflict(
          "criterion_conflict", "Acceptance-criterion keys and sort orders must be unique.");
    }
  }

  /** Persists revision and returns its stored representation. */
  private void saveRevision(RequirementEntity requirement, UUID userId) {
    saveRevision(
        requirement, userId, criteria.findAllByRequirementIdOrderBySortOrder(requirement.getId()));
  }

  /** Captures the complete pre-change story and ordered criterion aggregate. */
  private void saveRevision(
      RequirementEntity requirement, UUID userId, List<AcceptanceCriterionEntity> orderedCriteria) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("title", requirement.getTitle());
    snapshot.put("userStory", requirement.getUserStory());
    snapshot.put("businessRequirements", requirement.getBusinessRequirements());
    snapshot.put("assumptions", requirement.getAssumptions());
    snapshot.put("sourceReference", requirement.getSourceReference());
    snapshot.put("status", requirement.getStatus().name());
    snapshot.put("priority", requirement.getPriority().name());
    snapshot.put("version", requirement.getVersion());
    snapshot.put(
        "acceptanceCriteria",
        orderedCriteria.stream()
            .map(
                item ->
                    Map.of(
                        "id", item.getId().toString(),
                        "criterionKey", item.getCriterionKey(),
                        "description", item.getDescription(),
                        "sortOrder", item.getSortOrder()))
            .toList());
    try {
      revisions.save(
          RequirementRevisionEntity.create(
              requirement.getId(),
              revisions.countByRequirementId(requirement.getId()) + 1,
              objectMapper.writeValueAsString(snapshot),
              userId,
              clock.instant()));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not save requirement revision.", exception);
    }
  }

  /** Asserts user managed status for the current operation. */
  private void assertUserManagedStatus(RequirementStatus status) {
    if (status == RequirementStatus.GENERATED || status == RequirementStatus.NEEDS_CLARIFICATION) {
      throw ApiExceptions.badRequest(
          "invalid_status_transition", "Generation-managed statuses cannot be set manually.");
    }
  }

  /** Rejects stale updates by comparing the submitted and persisted entity versions. */
  private void assertVersion(long actual, long requested, String resource) {
    if (actual != requested) {
      throw ApiExceptions.conflict(
          "stale_version", "This " + resource + " changed since it was loaded. Refresh and retry.");
    }
  }

  /** Normalizes key for the current operation. */
  private String normalizeKey(String value) {
    return value.strip().toUpperCase(Locale.ROOT);
  }

  /** Normalizes optional text before it is compared or persisted. */
  private String clean(String value) {
    return value == null ? "" : value.strip();
  }
}
