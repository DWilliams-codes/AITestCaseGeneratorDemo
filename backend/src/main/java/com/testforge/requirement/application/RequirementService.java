package com.testforge.requirement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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

  @Transactional(readOnly = true)
  public PageResponse<RequirementSummaryResponse> list(
      UUID ownerId, UUID projectId, int page, int size) {
    projectService.requireOwned(ownerId, projectId);
    return PageResponse.from(
        requirements
            .findAllByProjectIdOrderByUpdatedAtDesc(projectId, PageRequest.of(page, size))
            .map(this::toSummary));
  }

  @Transactional(readOnly = true)
  public RequirementResponse get(UUID ownerId, UUID requirementId) {
    return toResponse(requireOwned(ownerId, requirementId));
  }

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
        ownerId, projectId, "REQUIREMENT", requirement.getId(), "CREATED", Map.of());
    return toResponse(requirement);
  }

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
        clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "REQUIREMENT",
        requirementId,
        "UPDATED",
        Map.of("newStatus", request.status().name()));
    return toResponse(requirement);
  }

  @Transactional
  public AcceptanceCriterionResponse addCriterion(
      UUID ownerId, UUID requirementId, AcceptanceCriterionRequest request) {
    RequirementEntity requirement = requireOwned(ownerId, requirementId);
    List<AcceptanceCriterionEntity> existing =
        criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    if (existing.size() >= 50) {
      throw ApiExceptions.badRequest(
          "criteria_limit_exceeded", "A requirement may have at most 50 acceptance criteria.");
    }
    ensureCriterionUnique(existing, null, request);
    AcceptanceCriterionEntity criterion =
        criteria.save(
            AcceptanceCriterionEntity.create(
                requirementId,
                normalizeKey(request.criterionKey()),
                request.description().strip(),
                request.sortOrder(),
                clock.instant()));
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "ACCEPTANCE_CRITERION",
        criterion.getId(),
        "CREATED",
        Map.of("criterionKey", criterion.getCriterionKey()));
    return toCriterion(criterion);
  }

  @Transactional
  public AcceptanceCriterionResponse updateCriterion(
      UUID ownerId, UUID criterionId, AcceptanceCriterionRequest request) {
    AcceptanceCriterionEntity criterion =
        criteria
            .findOwned(criterionId, ownerId)
            .orElseThrow(() -> ApiExceptions.notFound("Acceptance criterion not found."));
    RequirementEntity requirement = requireOwned(ownerId, criterion.getRequirementId());
    ensureCriterionUnique(
        criteria.findAllByRequirementIdOrderBySortOrder(requirement.getId()), criterionId, request);
    criterion.update(
        normalizeKey(request.criterionKey()),
        request.description().strip(),
        request.sortOrder(),
        clock.instant());
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "ACCEPTANCE_CRITERION",
        criterionId,
        "UPDATED",
        Map.of("criterionKey", criterion.getCriterionKey()));
    return toCriterion(criterion);
  }

  @Transactional
  public void deleteCriterion(UUID ownerId, UUID criterionId) {
    AcceptanceCriterionEntity criterion =
        criteria
            .findOwned(criterionId, ownerId)
            .orElseThrow(() -> ApiExceptions.notFound("Acceptance criterion not found."));
    RequirementEntity requirement = requireOwned(ownerId, criterion.getRequirementId());
    if (criteria.countByRequirementId(requirement.getId()) <= 1) {
      throw ApiExceptions.badRequest(
          "minimum_criteria_required",
          "A requirement must keep at least one acceptance criterion.");
    }
    criteria.delete(criterion);
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "ACCEPTANCE_CRITERION",
        criterionId,
        "DELETED",
        Map.of("criterionKey", criterion.getCriterionKey()));
  }

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
        Map.of());
    return toAmbiguity(ambiguity);
  }

  @Transactional(readOnly = true)
  public RequirementEntity requireOwned(UUID ownerId, UUID requirementId) {
    return requirements
        .findOwned(requirementId, ownerId)
        .orElseThrow(() -> ApiExceptions.notFound("Requirement not found."));
  }

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

  private RequirementSummaryResponse toSummary(RequirementEntity requirement) {
    return new RequirementSummaryResponse(
        requirement.getId(),
        requirement.getWorkItemNumber(),
        requirement.getProjectId(),
        requirement.getTitle(),
        requirement.getStatus(),
        Math.toIntExact(criteria.countByRequirementId(requirement.getId())),
        requirement.getUpdatedAt(),
        requirement.getVersion());
  }

  private AcceptanceCriterionResponse toCriterion(AcceptanceCriterionEntity criterion) {
    return new AcceptanceCriterionResponse(
        criterion.getId(),
        criterion.getCriterionKey(),
        criterion.getDescription(),
        criterion.getSortOrder(),
        criterion.getCreatedAt(),
        criterion.getUpdatedAt());
  }

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

  private void saveRevision(RequirementEntity requirement, UUID userId) {
    Map<String, Object> snapshot =
        Map.of(
            "title", requirement.getTitle(),
            "userStory", requirement.getUserStory(),
            "businessRequirements", requirement.getBusinessRequirements(),
            "assumptions", requirement.getAssumptions(),
            "sourceReference", requirement.getSourceReference(),
            "status", requirement.getStatus().name(),
            "version", requirement.getVersion());
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

  private void assertUserManagedStatus(RequirementStatus status) {
    if (status == RequirementStatus.GENERATED || status == RequirementStatus.NEEDS_CLARIFICATION) {
      throw ApiExceptions.badRequest(
          "invalid_status_transition", "Generation-managed statuses cannot be set manually.");
    }
  }

  private void assertVersion(long actual, long requested, String resource) {
    if (actual != requested) {
      throw ApiExceptions.conflict(
          "stale_version", "This " + resource + " changed since it was loaded. Refresh and retry.");
    }
  }

  private String normalizeKey(String value) {
    return value.strip().toUpperCase(Locale.ROOT);
  }

  private String clean(String value) {
    return value == null ? "" : value.strip();
  }
}
