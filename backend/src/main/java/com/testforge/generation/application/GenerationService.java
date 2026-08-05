package com.testforge.generation.application;

import com.testforge.audit.application.AuditService;
import com.testforge.common.correlation.CorrelationIds;
import com.testforge.common.error.ApiExceptions;
import com.testforge.common.workitem.WorkItemNumberService;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationSetState;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.dto.GenerationRunResponse;
import com.testforge.generation.provider.TestGenerationProvider;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.repository.GenerationRunRepository;
import com.testforge.generation.validation.GenerationResultValidator;
import com.testforge.generation.validation.GenerationValidationException;
import com.testforge.project.application.ProjectService;
import com.testforge.project.domain.ProjectStatus;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.domain.AcceptanceCriterionEntity;
import com.testforge.requirement.domain.RequirementAmbiguityEntity;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.requirement.repository.RequirementAmbiguityRepository;
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
import com.testforge.traceability.domain.TraceabilityLinkEntity;
import com.testforge.traceability.repository.TraceabilityLinkRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationService {
  private static final String PROMPT_VERSION = "manual-test-v1";

  private final RequirementService requirementService;
  private final WorkItemNumberService workItemNumbers;
  private final ProjectService projectService;
  private final AcceptanceCriterionRepository criteria;
  private final RequirementAmbiguityRepository ambiguities;
  private final GenerationRunRepository runs;
  private final TestCaseRepository testCases;
  private final TestCasePreconditionRepository preconditions;
  private final TestStepRepository steps;
  private final TestDataItemRepository testData;
  private final TraceabilityLinkRepository traceability;
  private final TestGenerationProvider provider;
  private final GenerationResultValidator validator;
  private final ActiveGenerationSetResolver activeSets;
  private final TestCaseRevisionRepository revisions;
  private final TestCaseReviewRepository reviews;
  private final TestDataReferencePolicy testDataReferences;
  private final AuditService auditService;
  private final Clock clock;

  /** Initializes GenerationService with its required collaborators and domain state. */
  public GenerationService(
      RequirementService requirementService,
      WorkItemNumberService workItemNumbers,
      ProjectService projectService,
      AcceptanceCriterionRepository criteria,
      RequirementAmbiguityRepository ambiguities,
      GenerationRunRepository runs,
      TestCaseRepository testCases,
      TestCasePreconditionRepository preconditions,
      TestStepRepository steps,
      TestDataItemRepository testData,
      TraceabilityLinkRepository traceability,
      TestGenerationProvider provider,
      GenerationResultValidator validator,
      ActiveGenerationSetResolver activeSets,
      TestCaseRevisionRepository revisions,
      TestCaseReviewRepository reviews,
      TestDataReferencePolicy testDataReferences,
      AuditService auditService,
      Clock clock) {
    this.requirementService = requirementService;
    this.workItemNumbers = workItemNumbers;
    this.projectService = projectService;
    this.criteria = criteria;
    this.ambiguities = ambiguities;
    this.runs = runs;
    this.testCases = testCases;
    this.preconditions = preconditions;
    this.steps = steps;
    this.testData = testData;
    this.traceability = traceability;
    this.provider = provider;
    this.validator = validator;
    this.activeSets = activeSets;
    this.revisions = revisions;
    this.reviews = reviews;
    this.testDataReferences = testDataReferences;
    this.auditService = auditService;
    this.clock = clock;
  }

  /** Generates structured manual test coverage from the requirement input. */
  @Transactional
  public GenerationRunResponse generate(UUID userId, UUID requirementId, String idempotencyKey) {
    return generateInternal(userId, requirementId, idempotencyKey, false);
  }

  /** Regenerates while requiring explicit confirmation before superseding reviewed output. */
  @Transactional
  public GenerationRunResponse regenerate(
      UUID userId, UUID requirementId, String idempotencyKey, boolean confirmSupersede) {
    return generateInternal(userId, requirementId, idempotencyKey, confirmSupersede);
  }

  /** Returns an idempotent retry before supersession checks so retries need no new confirmation. */
  private GenerationRunResponse generateInternal(
      UUID userId, UUID requirementId, String idempotencyKey, boolean confirmSupersede) {
    RequirementEntity requirement = requirementService.requireOwned(userId, requirementId);
    String idempotencyHash = hash(idempotencyKey);
    var existing =
        runs.findByRequirementIdAndRequestedByAndIdempotencyKeyHash(
            requirementId, userId, idempotencyHash);
    if (existing.isPresent()) {
      return toResponse(existing.get());
    }
    requireSupersessionConfirmation(requirementId, confirmSupersede);
    if (projectService.requireOwned(userId, requirement.getProjectId()).getStatus()
        == ProjectStatus.ARCHIVED) {
      throw ApiExceptions.badRequest(
          "project_archived", "Archived projects cannot generate new test cases.");
    }
    List<AcceptanceCriterionEntity> criterionEntities =
        criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    if (criterionEntities.isEmpty()) {
      throw ApiExceptions.badRequest(
          "acceptance_criteria_required",
          "Add at least one acceptance criterion before generation.");
    }
    TestGenerationRequest providerRequest = toProviderRequest(requirement, criterionEntities);
    Instant now = clock.instant();
    GenerationRunEntity run =
        runs.save(
            GenerationRunEntity.pending(
                requirementId,
                userId,
                provider.providerName(),
                provider.modelName(),
                PROMPT_VERSION,
                hash(canonicalInput(providerRequest)),
                idempotencyHash,
                CorrelationIds.current(),
                now));
    try {
      TestGenerationResult result = generateValidated(providerRequest);
      persistResult(requirement, criterionEntities, run, result, userId);
      run.complete(
          result.testCases().size(),
          result.usage() == null ? 0 : result.usage().inputTokens(),
          result.usage() == null ? 0 : result.usage().outputTokens(),
          clock.instant());
      requirement.markGenerated(!result.ambiguities().isEmpty(), clock.instant());
      auditService.record(
          userId,
          requirement.getProjectId(),
          "GENERATION_RUN",
          run.getId(),
          "COMPLETED",
          Map.of("caseCount", result.testCases().size(), "provider", provider.providerName()));
    } catch (GenerationValidationException exception) {
      run.fail(
          GenerationStatus.REJECTED_BY_VALIDATION,
          "invalid_provider_output",
          safeMessage(exception),
          clock.instant());
    } catch (RuntimeException exception) {
      run.fail(
          GenerationStatus.FAILED,
          "provider_failure",
          "Test-case generation failed safely.",
          clock.instant());
    }
    return toResponse(run);
  }

  /** Requires confirmation before a new operation supersedes protected active evidence. */
  private void requireSupersessionConfirmation(UUID requirementId, boolean confirmSupersede) {
    activeSets
        .resolve(requirementId)
        .filter(
            run ->
                revisions.countByGenerationRunId(run.getId()) > 0
                    || reviews.countByGenerationRunId(run.getId()) > 0)
        .filter(run -> !confirmSupersede)
        .ifPresent(
            run -> {
              throw ApiExceptions.conflict(
                  "supersede_confirmation_required",
                  "Confirm superseding the active generation set because it contains review or revision evidence.");
            });
  }

  /** Returns the owned resource identified by the request. */
  @Transactional(readOnly = true)
  public GenerationRunResponse get(UUID userId, UUID runId) {
    return runs.findOwned(runId, userId)
        .map(this::toResponse)
        .orElseThrow(() -> ApiExceptions.notFound("Generation run not found."));
  }

  /** Lists generation attempts for an owned user story in reverse chronological order. */
  @Transactional(readOnly = true)
  public List<GenerationRunResponse> list(UUID userId, UUID requirementId) {
    requirementService.requireOwned(userId, requirementId);
    return runs.findAllByRequirementIdOrderByStartedAtDesc(requirementId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Validates every provider result and permits exactly one retry after semantic rejection. */
  private TestGenerationResult generateValidated(TestGenerationRequest request) {
    GenerationValidationException firstFailure;
    try {
      TestGenerationResult result = provider.generate(request);
      validator.validate(request, result);
      return result;
    } catch (GenerationValidationException exception) {
      firstFailure = exception;
    }
    TestGenerationResult retry = provider.generate(request);
    try {
      validator.validate(request, retry);
      return retry;
    } catch (GenerationValidationException exception) {
      exception.addSuppressed(firstFailure);
      throw exception;
    }
  }

  /** Persists only validated structured output inside the surrounding transaction. */
  private void persistResult(
      RequirementEntity requirement,
      List<AcceptanceCriterionEntity> criterionEntities,
      GenerationRunEntity run,
      TestGenerationResult result,
      UUID userId) {
    ambiguities.deleteAllByRequirementIdAndResolvedFalse(requirement.getId());
    Instant now = clock.instant();
    for (var ambiguity : result.ambiguities()) {
      ambiguities.save(
          RequirementAmbiguityEntity.create(
              requirement.getId(),
              ambiguity.category(),
              ambiguity.description(),
              ambiguity.severity(),
              ambiguity.suggestedQuestion(),
              now));
    }
    Map<String, AcceptanceCriterionEntity> byKey = new HashMap<>();
    criterionEntities.forEach(item -> byKey.put(item.getCriterionKey(), item));
    for (GeneratedTestCase generated : result.testCases()) {
      long workItemNumber = workItemNumbers.next();
      String key = "TC-" + workItemNumber;
      TestCaseEntity testCase =
          testCases.save(
              TestCaseEntity.create(
                  workItemNumber,
                  requirement.getId(),
                  run.getId(),
                  key,
                  generated.title(),
                  generated.objective(),
                  generated.category(),
                  generated.priority(),
                  generated.riskLevel(),
                  generated.automationCandidate(),
                  generated.coverageIntent(),
                  generated.rationale(),
                  generated.finalExpectedOutcome(),
                  userId,
                  now));
      persistParts(testCase.getId(), generated);
      for (String criterionKey : generated.acceptanceCriteriaKeys()) {
        AcceptanceCriterionEntity criterion = byKey.get(criterionKey);
        if (criterion != null) {
          CoverageType type =
              generated.coverageIntent()
                      == com.testforge.testcase.domain.CoverageIntent.ACCEPTANCE_CRITERIA
                  ? CoverageType.DIRECT
                  : CoverageType.SUPPORTING;
          traceability.save(
              TraceabilityLinkEntity.create(
                  criterion.getId(), testCase.getId(), type, new BigDecimal("0.9500"), now));
        }
      }
    }
  }

  /**
   * Resolves every test-data reference before the first case-part write to prevent partial
   * evidence.
   */
  private void persistParts(UUID testCaseId, GeneratedTestCase generated) {
    List<String> canonicalReferences =
        testDataReferences.canonicalize(
            generated.testData() == null
                ? List.of()
                : generated.testData().stream().map(item -> item.name().strip()).toList(),
            generated.steps().stream().map(item -> item.testDataReference()).toList());
    if (generated.preconditions() != null) {
      for (int index = 0; index < generated.preconditions().size(); index++) {
        preconditions.save(
            TestCasePreconditionEntity.create(
                testCaseId, index, generated.preconditions().get(index)));
      }
    }
    if (generated.testData() != null) {
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

  /** Maps the source data to provider request. */
  private TestGenerationRequest toProviderRequest(
      RequirementEntity requirement, List<AcceptanceCriterionEntity> criterionEntities) {
    return new TestGenerationRequest(
        requirement.getId(),
        requirement.getTitle(),
        requirement.getUserStory(),
        requirement.getBusinessRequirements(),
        requirement.getAssumptions(),
        criterionEntities.stream()
            .map(item -> new CriterionInput(item.getCriterionKey(), item.getDescription()))
            .toList(),
        CorrelationIds.current());
  }

  /** Maps the source data to response. */
  private GenerationRunResponse toResponse(GenerationRunEntity run) {
    var active = activeSets.resolve(run.getRequirementId());
    boolean successful = run.getStatus() == GenerationStatus.COMPLETED;
    return new GenerationRunResponse(
        run.getId(),
        run.getRequirementId(),
        run.getProvider(),
        run.getModel(),
        run.getPromptVersion(),
        run.getStatus(),
        run.getStartedAt(),
        run.getCompletedAt(),
        run.getLatencyMs(),
        run.getGeneratedCaseCount(),
        run.getInputTokens(),
        run.getOutputTokens(),
        run.getFailureCode(),
        run.getFailureMessage(),
        run.getCorrelationId(),
        activeSets.setNumber(run),
        successful
            ? (active.map(item -> item.getId().equals(run.getId())).orElse(false)
                ? GenerationSetState.ACTIVE
                : GenerationSetState.SUPERSEDED)
            : null);
  }

  /** Produces stable hash material from generation inputs without transport metadata. */
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

  /** Digests idempotency keys and input provenance instead of retaining their raw values. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }

  /** Bounds validator detail; provider transport failures follow the separate generic path. */
  private String safeMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return "Provider output did not satisfy the TestForge schema.";
    }
    return message.substring(0, Math.min(message.length(), 500));
  }
}
