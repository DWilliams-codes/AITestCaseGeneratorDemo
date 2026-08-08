package com.testforge.generation.application;

import com.testforge.audit.application.AuditMetadata;
import com.testforge.audit.application.AuditService;
import com.testforge.common.correlation.CorrelationIds;
import com.testforge.common.error.ApiExceptions;
import com.testforge.common.workitem.WorkItemNumberService;
import com.testforge.config.GenerationProperties;
import com.testforge.config.OpenAiProperties;
import com.testforge.generation.domain.GenerationCriterionSnapshotEntity;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.repository.GenerationCriterionSnapshotRepository;
import com.testforge.generation.repository.GenerationRunRepository;
import com.testforge.project.domain.ProjectStatus;
import com.testforge.project.repository.ProjectRepository;
import com.testforge.requirement.domain.AcceptanceCriterionEntity;
import com.testforge.requirement.domain.RequirementAmbiguityEntity;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.requirement.repository.RequirementAmbiguityRepository;
import com.testforge.requirement.repository.RequirementRepository;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.CoverageType;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCasePreconditionEntity;
import com.testforge.testcase.domain.TestDataItemEntity;
import com.testforge.testcase.domain.TestStepEntity;
import com.testforge.testcase.repository.TestCasePreconditionRepository;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.testcase.repository.TestCaseReviewRepository;
import com.testforge.testcase.repository.TestCaseRevisionRepository;
import com.testforge.testcase.repository.TestDataItemRepository;
import com.testforge.testcase.repository.TestStepRepository;
import com.testforge.testcase.validation.TestDataReferencePolicy;
import com.testforge.traceability.domain.SnapshotTraceabilityLinkEntity;
import com.testforge.traceability.domain.TraceabilityLinkEntity;
import com.testforge.traceability.repository.SnapshotTraceabilityLinkRepository;
import com.testforge.traceability.repository.TraceabilityLinkRepository;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns short database transactions around nontransactional provider work. */
@Service
public class GenerationTransactionService {
  private final RequirementRepository requirements;
  private final ProjectRepository projects;
  private final AcceptanceCriterionRepository criteria;
  private final RequirementAmbiguityRepository ambiguities;
  private final GenerationRunRepository runs;
  private final GenerationCriterionSnapshotRepository snapshots;
  private final TestCaseRepository testCases;
  private final TestCasePreconditionRepository preconditions;
  private final TestStepRepository steps;
  private final TestDataItemRepository testData;
  private final TraceabilityLinkRepository legacyLinks;
  private final SnapshotTraceabilityLinkRepository snapshotLinks;
  private final TestCaseRevisionRepository revisions;
  private final TestCaseReviewRepository reviews;
  private final WorkItemNumberService workItemNumbers;
  private final TestDataReferencePolicy testDataReferences;
  private final ActiveGenerationSetResolver activeSets;
  private final AuditService audit;
  private final Clock clock;
  private final Duration staleTimeout;
  private final int providerConnectTimeoutSeconds;
  private final int providerReadTimeoutSeconds;

  /** Initializes GenerationTransactionService with its required collaborators and domain state. */
  public GenerationTransactionService(
      RequirementRepository requirements,
      ProjectRepository projects,
      AcceptanceCriterionRepository criteria,
      RequirementAmbiguityRepository ambiguities,
      GenerationRunRepository runs,
      GenerationCriterionSnapshotRepository snapshots,
      TestCaseRepository testCases,
      TestCasePreconditionRepository preconditions,
      TestStepRepository steps,
      TestDataItemRepository testData,
      TraceabilityLinkRepository legacyLinks,
      SnapshotTraceabilityLinkRepository snapshotLinks,
      TestCaseRevisionRepository revisions,
      TestCaseReviewRepository reviews,
      WorkItemNumberService workItemNumbers,
      TestDataReferencePolicy testDataReferences,
      ActiveGenerationSetResolver activeSets,
      AuditService audit,
      Clock clock,
      GenerationProperties generationProperties,
      OpenAiProperties openAiProperties) {
    this.staleTimeout = generationProperties.stalePendingTimeout();
    this.providerConnectTimeoutSeconds = openAiProperties.connectTimeoutSeconds();
    this.providerReadTimeoutSeconds = openAiProperties.readTimeoutSeconds();
    this.requirements = requirements;
    this.projects = projects;
    this.criteria = criteria;
    this.ambiguities = ambiguities;
    this.runs = runs;
    this.snapshots = snapshots;
    this.testCases = testCases;
    this.preconditions = preconditions;
    this.steps = steps;
    this.testData = testData;
    this.legacyLinks = legacyLinks;
    this.snapshotLinks = snapshotLinks;
    this.revisions = revisions;
    this.reviews = reviews;
    this.workItemNumbers = workItemNumbers;
    this.testDataReferences = testDataReferences;
    this.activeSets = activeSets;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Rejects a claim timeout that could expire while the bounded retry sequence is still running.
   */
  @PostConstruct
  void validateStaleTimeout() {
    Duration providerBudget =
        Duration.ofSeconds(2L * (providerConnectTimeoutSeconds + providerReadTimeoutSeconds));
    if (staleTimeout.compareTo(providerBudget) <= 0) {
      throw new IllegalArgumentException(
          "generation stale-pending-timeout must exceed two configured provider call bounds");
    }
  }

  /**
   * Atomically claims an idempotency key and captures exact source criteria before provider work.
   */
  @Transactional
  public Claim claim(
      UUID userId,
      UUID requirementId,
      String idempotencyHash,
      boolean confirmSupersede,
      String provider,
      String model,
      String adapterVersion) {
    RequirementEntity requirement =
        requirements
            .findOwnedForUpdate(requirementId, userId)
            .orElseThrow(() -> ApiExceptions.notFound("User Story not found."));
    Instant now = clock.instant();
    GenerationRunEntity existing =
        runs.findByRequirementIdAndRequestedByAndIdempotencyKeyHash(
                requirementId, userId, idempotencyHash)
            .orElse(null);
    if (existing != null) {
      expireIfStale(existing, now);
      return Claim.existing(existing);
    }
    requireSupersessionConfirmation(requirementId, confirmSupersede);
    if (projects.findById(requirement.getProjectId()).orElseThrow().getStatus()
        == ProjectStatus.ARCHIVED) {
      throw ApiExceptions.badRequest(
          "project_archived", "Archived projects cannot generate new test cases.");
    }
    List<AcceptanceCriterionEntity> sourceCriteria =
        criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    if (sourceCriteria.isEmpty()) {
      throw ApiExceptions.badRequest(
          "acceptance_criteria_required",
          "Add at least one acceptance criterion before generation.");
    }
    TestGenerationRequest request = toProviderRequest(requirement, sourceCriteria);
    GenerationRunEntity run =
        GenerationRunEntity.pending(
            requirementId,
            userId,
            provider,
            model,
            GenerationContractVersions.PROMPT,
            hash(canonicalInput(request)),
            idempotencyHash,
            CorrelationIds.current(),
            now);
    run.recordRelease(
        adapterVersion,
        GenerationContractVersions.RESULT,
        GenerationContractVersions.SCHEMA,
        GenerationContractVersions.VALIDATOR,
        requirement.getVersion());
    runs.saveAndFlush(run);
    List<GenerationCriterionSnapshotEntity> savedSnapshots =
        snapshots.saveAllAndFlush(
            sourceCriteria.stream()
                .map(
                    criterion ->
                        GenerationCriterionSnapshotEntity.exact(
                            run.getId(),
                            criterion.getId(),
                            criterion.getCriterionKey(),
                            criterion.getDescription(),
                            criterion.getSortOrder(),
                            requirement.getVersion()))
                .toList());
    return Claim.claimed(
        run,
        request,
        savedSnapshots.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    GenerationCriterionSnapshotEntity::getCriterionKey,
                    snapshot ->
                        new CapturedCriterion(
                            snapshot.getId(), snapshot.getSourceAcceptanceCriterionId()))));
  }

  /** Finalizes a still-pending run and all validated evidence in one transaction. */
  @Transactional
  public GenerationRunEntity complete(Claim claim, TestGenerationResult result) {
    GenerationRunEntity run =
        runs.findByIdForUpdate(claim.run().getId())
            .orElseThrow(() -> ApiExceptions.notFound("Generation run not found."));
    if (run.getStatus() != GenerationStatus.PENDING) return run;
    RequirementEntity requirement =
        requirements
            .findOwnedForUpdate(run.getRequirementId(), run.getRequestedBy())
            .orElseThrow(() -> ApiExceptions.notFound("User Story not found."));
    List<AcceptanceCriterionEntity> currentCriteria =
        criteria.findAllByRequirementIdOrderBySortOrder(requirement.getId());
    Map<UUID, AcceptanceCriterionEntity> legacyCriteriaById =
        currentCriteria.stream()
            .collect(Collectors.toMap(AcceptanceCriterionEntity::getId, Function.identity()));
    Instant now = clock.instant();
    if (!legacyCriteriaById
        .keySet()
        .containsAll(
            claim.capturedCriteriaByKey().values().stream()
                .map(CapturedCriterion::sourceCriterionId)
                .toList())) {
      run.fail(
          GenerationStatus.FAILED,
          "source_criteria_changed",
          "Source acceptance criteria changed while generation was in progress.",
          now);
      return run;
    }
    ambiguities.deleteAllByRequirementIdAndResolvedFalse(requirement.getId());
    result
        .ambiguities()
        .forEach(
            ambiguity ->
                ambiguities.save(
                    RequirementAmbiguityEntity.create(
                        requirement.getId(),
                        ambiguity.category(),
                        ambiguity.description(),
                        ambiguity.severity(),
                        ambiguity.suggestedQuestion(),
                        now)));
    for (GeneratedTestCase generated : result.testCases()) {
      long workItemNumber = workItemNumbers.next();
      TestCaseEntity testCase =
          testCases.save(
              TestCaseEntity.create(
                  workItemNumber,
                  requirement.getId(),
                  run.getId(),
                  "TC-" + workItemNumber,
                  generated.title(),
                  generated.objective(),
                  generated.category(),
                  generated.priority(),
                  generated.riskLevel(),
                  generated.automationCandidate(),
                  generated.coverageIntent(),
                  generated.rationale(),
                  generated.finalExpectedOutcome(),
                  run.getRequestedBy(),
                  now));
      persistParts(testCase.getId(), generated);
      for (String criterionKey : generated.acceptanceCriteriaKeys()) {
        CoverageType coverageType =
            generated.coverageIntent() == CoverageIntent.ACCEPTANCE_CRITERIA
                ? CoverageType.DIRECT
                : CoverageType.SUPPORTING;
        CapturedCriterion capturedCriterion = claim.capturedCriteriaByKey().get(criterionKey);
        if (capturedCriterion == null) {
          throw new IllegalStateException("Generation snapshot key is unavailable.");
        }
        snapshotLinks.save(
            SnapshotTraceabilityLinkEntity.create(
                capturedCriterion.snapshotId(),
                testCase.getId(),
                coverageType,
                new BigDecimal("0.9500"),
                now));
        AcceptanceCriterionEntity legacyCriterion =
            legacyCriteriaById.get(capturedCriterion.sourceCriterionId());
        if (legacyCriterion != null) {
          legacyLinks.save(
              TraceabilityLinkEntity.create(
                  legacyCriterion.getId(),
                  testCase.getId(),
                  coverageType,
                  new BigDecimal("0.9500"),
                  now));
        }
      }
    }
    run.complete(
        result.testCases().size(),
        result.usage() == null ? 0 : result.usage().inputTokens(),
        result.usage() == null ? 0 : result.usage().outputTokens(),
        now);
    requirement.markGenerated(!result.ambiguities().isEmpty(), now);
    audit.record(
        run.getRequestedBy(),
        requirement.getProjectId(),
        "GENERATION_RUN",
        run.getId(),
        "COMPLETED",
        AuditMetadata.generated(result.testCases().size(), run.getProvider()));
    return run;
  }

  /** Records a bounded safe terminal failure without retaining partial generated evidence. */
  @Transactional
  public GenerationRunEntity fail(
      UUID runId, GenerationStatus status, String code, String safeMessage) {
    GenerationRunEntity run =
        runs.findByIdForUpdate(runId)
            .orElseThrow(() -> ApiExceptions.notFound("Generation run not found."));
    if (run.getStatus() == GenerationStatus.PENDING) {
      run.fail(status, code, safeMessage, clock.instant());
    }
    return run;
  }

  /** Returns one owned run and terminalizes an abandoned pending claim. */
  @Transactional
  public GenerationRunEntity getOwned(UUID userId, UUID runId) {
    runs.findOwned(runId, userId)
        .orElseThrow(() -> ApiExceptions.notFound("Generation run not found."));
    GenerationRunEntity locked = runs.findByIdForUpdate(runId).orElseThrow();
    expireIfStale(locked, clock.instant());
    return locked;
  }

  /** Returns bounded caller-owned run history after expiring abandoned claims. */
  @Transactional
  public List<GenerationRunEntity> listOwned(UUID userId, UUID requirementId) {
    requirements
        .findOwnedForUpdate(requirementId, userId)
        .orElseThrow(() -> ApiExceptions.notFound("User Story not found."));
    List<GenerationRunEntity> history =
        runs.findAllByRequirementIdOrderByStartedAtDesc(
                requirementId, org.springframework.data.domain.PageRequest.of(0, 100))
            .getContent();
    Instant now = clock.instant();
    history.forEach(run -> expireIfStale(run, now));
    return history;
  }

  /** Returns one bounded caller-owned run-history page. */
  @Transactional
  public Page<GenerationRunEntity> listOwnedPage(
      UUID userId, UUID requirementId, Pageable pageable) {
    requirements
        .findOwnedForUpdate(requirementId, userId)
        .orElseThrow(() -> ApiExceptions.notFound("User Story not found."));
    Page<GenerationRunEntity> history =
        runs.findAllByRequirementIdOrderByStartedAtDesc(requirementId, pageable);
    Instant now = clock.instant();
    history.forEach(run -> expireIfStale(run, now));
    return history;
  }

  /** Executes the expire if stale operation for GenerationTransactionService. */
  private void expireIfStale(GenerationRunEntity run, Instant now) {
    if (run.getStatus() == GenerationStatus.PENDING
        && !run.getStartedAt().plus(staleTimeout).isAfter(now)) {
      run.fail(
          GenerationStatus.FAILED,
          "stale_generation_claim",
          "The generation claim expired before it completed.",
          now);
    }
  }

  /** Requires supersession confirmation for the current operation. */
  private void requireSupersessionConfirmation(UUID requirementId, boolean confirmed) {
    activeSets
        .resolve(requirementId)
        .filter(
            active ->
                revisions.countByGenerationRunId(active.getId()) > 0
                    || reviews.countByGenerationRunId(active.getId()) > 0)
        .filter(active -> !confirmed)
        .ifPresent(
            active -> {
              throw ApiExceptions.conflict(
                  "supersede_confirmation_required",
                  "Confirm superseding the active generation set because it contains review or revision evidence.");
            });
  }

  /** Maps the source data to provider request. */
  private TestGenerationRequest toProviderRequest(
      RequirementEntity requirement, List<AcceptanceCriterionEntity> sourceCriteria) {
    return new TestGenerationRequest(
        requirement.getId(),
        requirement.getTitle(),
        requirement.getUserStory(),
        requirement.getBusinessRequirements(),
        requirement.getAssumptions(),
        sourceCriteria.stream()
            .map(item -> new CriterionInput(item.getCriterionKey(), item.getDescription()))
            .toList(),
        CorrelationIds.current());
  }

  /** Executes the canonical input operation for GenerationTransactionService. */
  private String canonicalInput(TestGenerationRequest request) {
    StringBuilder value =
        new StringBuilder()
            .append(request.title())
            .append('\n')
            .append(request.userStory())
            .append('\n')
            .append(request.businessRequirements())
            .append('\n')
            .append(request.assumptions());
    request
        .acceptanceCriteria()
        .forEach(
            item -> value.append('\n').append(item.key()).append(':').append(item.description()));
    return value.toString();
  }

  /** Reports whether the result h. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }

  /** Executes the persist parts operation for GenerationTransactionService. */
  private void persistParts(UUID testCaseId, GeneratedTestCase generated) {
    List<String> canonicalReferences =
        testDataReferences.canonicalize(
            generated.testData().stream().map(item -> item.name().strip()).toList(),
            generated.steps().stream().map(item -> item.testDataReference()).toList());
    for (int index = 0; index < generated.preconditions().size(); index++) {
      preconditions.save(
          TestCasePreconditionEntity.create(
              testCaseId, index, generated.preconditions().get(index)));
    }
    for (var data : generated.testData()) {
      testData.save(
          TestDataItemEntity.create(
              testCaseId,
              data.name().strip(),
              data.description(),
              data.exampleValue(),
              data.sensitivity(),
              data.generationStrategy()));
    }
    for (int index = 0; index < generated.steps().size(); index++) {
      var step = generated.steps().get(index);
      steps.save(
          TestStepEntity.create(
              testCaseId,
              step.stepNumber(),
              step.action(),
              step.expectedResult(),
              canonicalReferences.get(index)));
    }
  }

  public record Claim(
      GenerationRunEntity run,
      TestGenerationRequest providerRequest,
      Map<String, CapturedCriterion> capturedCriteriaByKey,
      boolean existing) {
    /** Copies mutable container input at the transaction boundary. */
    public Claim {
      capturedCriteriaByKey = Map.copyOf(capturedCriteriaByKey);
    }

    /** Returns immutable snapshot and source-identity evidence captured before provider work. */
    @Override
    public Map<String, CapturedCriterion> capturedCriteriaByKey() {
      return Map.copyOf(capturedCriteriaByKey);
    }

    /** Executes the existing operation for Claim. */
    static Claim existing(GenerationRunEntity run) {
      return new Claim(run, null, Map.of(), true);
    }

    /** Executes the claimed operation for Claim. */
    static Claim claimed(
        GenerationRunEntity run,
        TestGenerationRequest request,
        Map<String, CapturedCriterion> capturedCriteriaByKey) {
      return new Claim(run, request, capturedCriteriaByKey, false);
    }
  }

  /** Binds one immutable snapshot to its retained source criterion identity. */
  public record CapturedCriterion(long snapshotId, UUID sourceCriterionId) {}
}
