package com.testforge.testcase.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.application.AuditMetadata;
import com.testforge.audit.application.AuditService;
import com.testforge.audit.application.LegacyReopenAuditReconciler;
import com.testforge.common.dto.PageResponse;
import com.testforge.common.error.ApiExceptions;
import com.testforge.generation.application.ActiveGenerationSetResolver;
import com.testforge.generation.application.LegacyGenerationEvidenceReconciler;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.repository.GenerationRunRepository;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.testcase.domain.ReviewDecision;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCasePreconditionEntity;
import com.testforge.testcase.domain.TestCaseReviewEntity;
import com.testforge.testcase.domain.TestCaseRevisionEntity;
import com.testforge.testcase.domain.TestDataItemEntity;
import com.testforge.testcase.domain.TestStepEntity;
import com.testforge.testcase.dto.TestCaseDtos.PreconditionResponse;
import com.testforge.testcase.dto.TestCaseDtos.ReopenRequest;
import com.testforge.testcase.dto.TestCaseDtos.ReviewRequest;
import com.testforge.testcase.dto.TestCaseDtos.ReviewResponse;
import com.testforge.testcase.dto.TestCaseDtos.RevisionResponse;
import com.testforge.testcase.dto.TestCaseDtos.TestCaseResponse;
import com.testforge.testcase.dto.TestCaseDtos.UpdateTestCaseRequest;
import com.testforge.testcase.repository.TestCasePreconditionRepository;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.testcase.repository.TestCaseReviewRepository;
import com.testforge.testcase.repository.TestCaseRevisionRepository;
import com.testforge.testcase.repository.TestDataItemRepository;
import com.testforge.testcase.repository.TestStepRepository;
import com.testforge.testcase.validation.TestDataReferencePolicy;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseService {
  private final TestCaseRepository testCases;
  private final TestCasePreconditionRepository preconditions;
  private final TestStepRepository steps;
  private final TestDataItemRepository testData;
  private final TestCaseReviewRepository reviews;
  private final TestCaseRevisionRepository revisions;
  private final RequirementService requirementService;
  private final ActiveGenerationSetResolver activeSets;
  private final LegacyGenerationEvidenceReconciler legacyEvidence;
  private final GenerationRunRepository generationRuns;
  private final TestCaseResponseAssembler responseAssembler;
  private final TestDataReferencePolicy testDataReferences;
  private final AuditService auditService;
  private final LegacyReopenAuditReconciler legacyReopens;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Initializes TestCaseService with its required collaborators and domain state. */
  public TestCaseService(
      TestCaseRepository testCases,
      TestCasePreconditionRepository preconditions,
      TestStepRepository steps,
      TestDataItemRepository testData,
      TestCaseReviewRepository reviews,
      TestCaseRevisionRepository revisions,
      RequirementService requirementService,
      ActiveGenerationSetResolver activeSets,
      LegacyGenerationEvidenceReconciler legacyEvidence,
      GenerationRunRepository generationRuns,
      TestCaseResponseAssembler responseAssembler,
      TestDataReferencePolicy testDataReferences,
      AuditService auditService,
      LegacyReopenAuditReconciler legacyReopens,
      ObjectMapper objectMapper,
      Clock clock) {
    this.testCases = testCases;
    this.preconditions = preconditions;
    this.steps = steps;
    this.testData = testData;
    this.reviews = reviews;
    this.revisions = revisions;
    this.requirementService = requirementService;
    this.activeSets = activeSets;
    this.legacyEvidence = legacyEvidence;
    this.generationRuns = generationRuns;
    this.responseAssembler = responseAssembler;
    this.testDataReferences = testDataReferences;
    this.auditService = auditService;
    this.legacyReopens = legacyReopens;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Lists resources visible to the current owner using the requested page. */
  @Transactional(readOnly = true)
  public List<TestCaseResponse> list(UUID ownerId, UUID requirementId, UUID generationRunId) {
    requirementService.requireOwned(ownerId, requirementId);
    GenerationRunEntity selectedRun = selectReadableRun(requirementId, generationRunId);
    if (selectedRun == null) {
      return List.of();
    }
    ensureLegacyEvidence(selectedRun);
    return responseAssembler.assembleAll(
        testCases
            .findAllByRequirementIdAndGenerationRunIdOrderByWorkItemNumber(
                requirementId, selectedRun.getId(), PageRequest.of(0, 100))
            .getContent());
  }

  /** Returns a canonical bounded page for one selected immutable generation set. */
  @Transactional(readOnly = true)
  public PageResponse<TestCaseResponse> listPage(
      UUID ownerId, UUID requirementId, UUID generationRunId, int page, int size) {
    requirementService.requireOwned(ownerId, requirementId);
    GenerationRunEntity selectedRun = selectReadableRun(requirementId, generationRunId);
    if (selectedRun == null) {
      return new PageResponse<>(List.of(), page, size, 0, 0, false);
    }
    ensureLegacyEvidence(selectedRun);
    var testCasePage =
        testCases.findAllByRequirementIdAndGenerationRunIdOrderByWorkItemNumber(
            requirementId, selectedRun.getId(), PageRequest.of(page, size));
    return new PageResponse<>(
        responseAssembler.assembleAll(testCasePage.getContent()),
        testCasePage.getNumber(),
        testCasePage.getSize(),
        testCasePage.getTotalElements(),
        testCasePage.getTotalPages(),
        testCasePage.hasNext());
  }

  /** Returns the owned resource identified by the request. */
  @Transactional(readOnly = true)
  public TestCaseResponse get(UUID ownerId, UUID testCaseId) {
    TestCaseEntity testCase = requireOwned(ownerId, testCaseId);
    generationRuns.findById(testCase.getGenerationRunId()).ifPresent(this::ensureLegacyEvidence);
    return toResponse(testCase);
  }

  /** Applies a validated update while preserving concurrency guarantees. */
  @Transactional
  public TestCaseResponse update(UUID ownerId, UUID testCaseId, UpdateTestCaseRequest request) {
    TestCaseEntity testCase = requireOwned(ownerId, testCaseId);
    assertActive(testCase);
    if (testCase.getVersion() != request.version()) {
      throw ApiExceptions.conflict(
          "stale_version", "This test case changed since it was loaded. Refresh and retry.");
    }
    validateStepNumbers(request);
    List<String> canonicalReferences = validateTestDataReferences(request);
    RequirementEntity requirement =
        requirementService.requireOwned(ownerId, testCase.getRequirementId());
    boolean actualChange = hasActualChange(testCase, request, canonicalReferences);
    if (!actualChange) {
      return toResponse(testCase);
    }
    saveRevision(testCase, ownerId);
    try {
      testCase.update(
          request.title().strip(),
          request.objective().strip(),
          request.category(),
          request.priority(),
          request.riskLevel(),
          request.automationCandidate(),
          request.rationale().strip(),
          request.finalExpectedOutcome().strip(),
          actualChange,
          clock.instant());
    } catch (TestCaseEntity.InvalidTransition exception) {
      throw ApiExceptions.conflict("invalid_test_case_transition", exception.getMessage());
    }
    replaceParts(testCaseId, request, canonicalReferences);
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "TEST_CASE",
        testCaseId,
        "UPDATED",
        AuditMetadata.testCase(testCase.getTestCaseKey()));
    testCases.flush();
    return toResponse(testCase);
  }

  /** Executes the review operation for TestCaseService. */
  @Transactional
  public TestCaseResponse review(
      UUID ownerId, UUID testCaseId, ReviewDecision decision, ReviewRequest request) {
    TestCaseEntity testCase = requireOwned(ownerId, testCaseId);
    assertActive(testCase);
    assertVersion(testCase, request.version());
    RequirementEntity requirement =
        requirementService.requireOwned(ownerId, testCase.getRequirementId());
    try {
      testCase.review(decision, clean(request.comments()), clock.instant());
    } catch (TestCaseEntity.InvalidTransition exception) {
      throw ApiExceptions.conflict("invalid_test_case_transition", exception.getMessage());
    }
    reviews.save(
        TestCaseReviewEntity.create(
            testCaseId, ownerId, decision, clean(request.comments()), clock.instant()));
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "TEST_CASE",
        testCaseId,
        "REVIEWED",
        AuditMetadata.reviewed(decision.name(), testCase.getTestCaseKey()));
    reviews.flush();
    testCases.flush();
    return toResponse(testCase);
  }

  /** Explicitly reopens a terminal active-set case with optimistic concurrency. */
  @Transactional
  public TestCaseResponse reopen(UUID ownerId, UUID testCaseId, ReopenRequest request) {
    TestCaseEntity testCase = requireOwned(ownerId, testCaseId);
    assertActive(testCase);
    assertVersion(testCase, request.version());
    RequirementEntity requirement =
        requirementService.requireOwned(ownerId, testCase.getRequirementId());
    saveReopenRevision(testCase, ownerId, request.reason().strip());
    try {
      testCase.reopen(request.reason().strip(), clock.instant());
    } catch (TestCaseEntity.InvalidTransition exception) {
      throw ApiExceptions.conflict("invalid_test_case_transition", exception.getMessage());
    }
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "TEST_CASE",
        testCaseId,
        "REOPENED",
        AuditMetadata.reopened(testCase.getTestCaseKey()));
    testCases.flush();
    return toResponse(testCase);
  }

  /** Returns normalized, structured, owner-isolated revision history. */
  @Transactional(readOnly = true)
  public PageResponse<RevisionResponse> revisions(
      UUID ownerId, UUID testCaseId, int page, int size) {
    requireOwned(ownerId, testCaseId);
    for (int batch = 0; batch < LegacyReopenAuditReconciler.MAX_READ_BATCHES; batch++) {
      if (legacyReopens.reconcileTestCase(testCaseId) == 0) break;
    }
    return PageResponse.from(
        revisions
            .findAllByTestCaseIdOrderByChangedAtDescIdDesc(testCaseId, PageRequest.of(page, size))
            .map(
                revision ->
                    new RevisionResponse(
                        revision.getId(),
                        revision.getRevisionNumber(),
                        normalizeSnapshot(revision.getSnapshotJson()),
                        revision.getChangedBy(),
                        revision.getChangedAt(),
                        revision.getChangeType(),
                        revision.getChangeReason())));
  }

  /** Returns review evidence independently from the capped embedded compatibility field. */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> reviews(UUID ownerId, UUID testCaseId, int page, int size) {
    requireOwned(ownerId, testCaseId);
    return PageResponse.from(
        reviews
            .findAllByTestCaseIdOrderByCreatedAtDesc(testCaseId, PageRequest.of(page, size))
            .map(this::toReviewResponse));
  }

  /** Loads the requested resource and verifies that it belongs to the current owner. */
  @Transactional(readOnly = true)
  public TestCaseEntity requireOwned(UUID ownerId, UUID testCaseId) {
    return testCases
        .findOwned(testCaseId, ownerId)
        .orElseThrow(() -> ApiExceptions.notFound("Test case not found."));
  }

  /** Maps the source data to response. */
  public TestCaseResponse toResponse(TestCaseEntity testCase) {
    return responseAssembler.assemble(testCase);
  }

  /** Maps the source data to review response. */
  private ReviewResponse toReviewResponse(TestCaseReviewEntity item) {
    return new ReviewResponse(
        item.getId(),
        item.getReviewerId(),
        item.getDecision(),
        item.getComments(),
        item.getCreatedAt());
  }

  /** Executes the replace parts operation for TestCaseService. */
  private void replaceParts(
      UUID testCaseId, UpdateTestCaseRequest request, List<String> canonicalReferences) {
    preconditions.deleteAllByTestCaseId(testCaseId);
    steps.deleteAllByTestCaseId(testCaseId);
    testData.deleteAllByTestCaseId(testCaseId);
    preconditions.flush();
    steps.flush();
    testData.flush();
    for (int index = 0; index < request.preconditions().size(); index++) {
      preconditions.save(
          TestCasePreconditionEntity.create(
              testCaseId, index, request.preconditions().get(index).strip()));
    }
    for (int index = 0; index < request.steps().size(); index++) {
      var item = request.steps().get(index);
      steps.save(
          TestStepEntity.create(
              testCaseId,
              item.stepNumber(),
              item.action().strip(),
              item.expectedResult().strip(),
              canonicalReferences.get(index)));
    }
    request
        .testData()
        .forEach(
            item ->
                testData.save(
                    TestDataItemEntity.create(
                        testCaseId,
                        item.name().strip(),
                        item.description().strip(),
                        item.exampleValue().strip(),
                        item.sensitivity(),
                        item.generationStrategy().strip())));
  }

  /** Validates names/references before any managed case state is mutated. */
  private List<String> validateTestDataReferences(UpdateTestCaseRequest request) {
    try {
      return testDataReferences.canonicalize(
          request.testData().stream().map(item -> item.name().strip()).toList(),
          request.steps().stream().map(item -> item.testDataReference()).toList());
    } catch (TestDataReferencePolicy.Violation violation) {
      throw ApiExceptions.badRequest("invalid_test_data_reference", violation.getMessage());
    }
  }

  /** Compares normalized mutable fields and parts to prevent a false revision transition. */
  private boolean hasActualChange(
      TestCaseEntity testCase, UpdateTestCaseRequest request, List<String> canonicalReferences) {
    TestCaseResponse current = toResponse(testCase);
    if (!Objects.equals(current.title(), request.title().strip())
        || !Objects.equals(current.objective(), request.objective().strip())
        || current.category() != request.category()
        || current.priority() != request.priority()
        || current.riskLevel() != request.riskLevel()
        || current.automationCandidate() != request.automationCandidate()
        || !Objects.equals(current.rationale(), request.rationale().strip())
        || !Objects.equals(
            current.finalExpectedOutcome(), request.finalExpectedOutcome().strip())) {
      return true;
    }
    List<String> requestedPreconditions =
        request.preconditions().stream().map(String::strip).toList();
    if (!current.preconditions().stream()
        .map(PreconditionResponse::description)
        .toList()
        .equals(requestedPreconditions)) {
      return true;
    }
    if (current.steps().size() != request.steps().size()) {
      return true;
    }
    for (int index = 0; index < request.steps().size(); index++) {
      var persisted = current.steps().get(index);
      var requested = request.steps().get(index);
      if (persisted.stepNumber() != requested.stepNumber()
          || !Objects.equals(persisted.action(), requested.action().strip())
          || !Objects.equals(persisted.expectedResult(), requested.expectedResult().strip())
          || !Objects.equals(persisted.testDataReference(), canonicalReferences.get(index))) {
        return true;
      }
    }
    List<TestDataComparable> persistedData =
        current.testData().stream()
            .map(
                item ->
                    new TestDataComparable(
                        testDataReferences.key(item.name()),
                        item.description(),
                        item.exampleValue(),
                        item.sensitivity().name(),
                        item.generationStrategy()))
            .sorted(Comparator.comparing(TestDataComparable::normalizedName))
            .toList();
    List<TestDataComparable> requestedData =
        request.testData().stream()
            .map(
                item ->
                    new TestDataComparable(
                        testDataReferences.key(item.name()),
                        item.description().strip(),
                        item.exampleValue().strip(),
                        item.sensitivity().name(),
                        item.generationStrategy().strip()))
            .sorted(Comparator.comparing(TestDataComparable::normalizedName))
            .toList();
    return !persistedData.equals(requestedData);
  }

  /** Selects the active set by default and validates an explicit historical selector. */
  private GenerationRunEntity selectReadableRun(UUID requirementId, UUID requestedRunId) {
    if (requestedRunId == null) {
      return activeSets.resolve(requirementId).orElse(null);
    }
    return activeSets
        .successful(requirementId, requestedRunId)
        .orElseThrow(() -> ApiExceptions.notFound("Successful generation set not found."));
  }

  /** Reconciles one authorized bridge-era run before its immutable evidence is read. */
  private void ensureLegacyEvidence(GenerationRunEntity run) {
    if (run.getSourceSnapshotProvenance() == null) {
      legacyEvidence.reconcile(List.of(run));
    }
  }

  /** Rejects mutation when the case is not part of the active successful set. */
  private void assertActive(TestCaseEntity testCase) {
    boolean active =
        activeSets
            .resolve(testCase.getRequirementId())
            .map(run -> run.getId().equals(testCase.getGenerationRunId()))
            .orElse(false);
    if (!active) {
      throw ApiExceptions.conflict(
          "superseded_generation_set", "Superseded generation sets are read-only.");
    }
  }

  /** Rejects a stale test-case mutation using the submitted expected version. */
  private void assertVersion(TestCaseEntity testCase, long requestedVersion) {
    if (testCase.getVersion() != requestedVersion) {
      throw ApiExceptions.conflict(
          "stale_version", "This test case changed since it was loaded. Refresh and retry.");
    }
  }

  /** Normalizes current and legacy revision payloads into a structured object. */
  private com.fasterxml.jackson.databind.JsonNode normalizeSnapshot(String snapshotJson) {
    try {
      var snapshot = objectMapper.readTree(snapshotJson);
      if (snapshot != null && snapshot.isTextual()) {
        snapshot = objectMapper.readTree(snapshot.textValue());
      }
      if (snapshot != null && snapshot.isObject()) {
        var normalized = snapshot.deepCopy();
        if (!normalized.has("schemaVersion")) {
          ((com.fasterxml.jackson.databind.node.ObjectNode) normalized).put("schemaVersion", 1);
        }
        return normalized;
      }
      var normalized = objectMapper.createObjectNode();
      normalized.put("schemaVersion", 1);
      normalized.set("legacySnapshot", snapshot);
      return normalized;
    } catch (JsonProcessingException exception) {
      var normalized = objectMapper.createObjectNode();
      normalized.put("schemaVersion", 1);
      normalized.put("legacySnapshot", snapshotJson);
      return normalized;
    }
  }

  /** Executes the validate step numbers operation for TestCaseService. */
  private void validateStepNumbers(UpdateTestCaseRequest request) {
    if (request.steps().isEmpty()) {
      throw ApiExceptions.badRequest(
          "steps_required", "A test case must contain at least one step.");
    }
    for (int index = 0; index < request.steps().size(); index++) {
      if (request.steps().get(index).stepNumber() != index + 1) {
        throw ApiExceptions.badRequest(
            "invalid_step_order", "Step numbers must be contiguous and start at one.");
      }
    }
  }

  /** Persists revision and returns its stored representation. */
  private void saveRevision(TestCaseEntity testCase, UUID ownerId) {
    try {
      revisions.save(
          TestCaseRevisionEntity.create(
              testCase.getId(),
              revisions.countByTestCaseId(testCase.getId()) + 1,
              objectMapper.writeValueAsString(toResponse(testCase)),
              ownerId,
              clock.instant()));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not save test-case revision.", exception);
    }
  }

  /** Preserves the terminal state and controlled reason before reopening. */
  private void saveReopenRevision(TestCaseEntity testCase, UUID ownerId, String reason) {
    try {
      revisions.save(
          TestCaseRevisionEntity.reopen(
              testCase.getId(),
              revisions.countByTestCaseId(testCase.getId()) + 1,
              objectMapper.writeValueAsString(toResponse(testCase)),
              ownerId,
              clock.instant(),
              reason));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not save test-case reopen revision.", exception);
    }
  }

  /** Normalizes optional text before it is compared or persisted. */
  private String clean(String value) {
    return value == null ? "" : value.strip();
  }

  private record TestDataComparable(
      String normalizedName,
      String description,
      String exampleValue,
      String sensitivity,
      String generationStrategy) {}
}
