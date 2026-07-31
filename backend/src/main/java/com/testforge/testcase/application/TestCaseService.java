package com.testforge.testcase.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.application.AuditService;
import com.testforge.common.error.ApiExceptions;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.testcase.domain.ReviewDecision;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCasePreconditionEntity;
import com.testforge.testcase.domain.TestCaseReviewEntity;
import com.testforge.testcase.domain.TestCaseRevisionEntity;
import com.testforge.testcase.domain.TestDataItemEntity;
import com.testforge.testcase.domain.TestStepEntity;
import com.testforge.testcase.dto.TestCaseDtos.PreconditionResponse;
import com.testforge.testcase.dto.TestCaseDtos.ReviewRequest;
import com.testforge.testcase.dto.TestCaseDtos.ReviewResponse;
import com.testforge.testcase.dto.TestCaseDtos.StepResponse;
import com.testforge.testcase.dto.TestCaseDtos.TestCaseResponse;
import com.testforge.testcase.dto.TestCaseDtos.TestDataResponse;
import com.testforge.testcase.dto.TestCaseDtos.UpdateTestCaseRequest;
import com.testforge.testcase.repository.TestCasePreconditionRepository;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.testcase.repository.TestCaseReviewRepository;
import com.testforge.testcase.repository.TestCaseRevisionRepository;
import com.testforge.testcase.repository.TestDataItemRepository;
import com.testforge.testcase.repository.TestStepRepository;
import com.testforge.traceability.repository.TraceabilityLinkRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final TraceabilityLinkRepository links;
  private final AcceptanceCriterionRepository criteria;
  private final RequirementService requirementService;
  private final AuditService auditService;
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
      TraceabilityLinkRepository links,
      AcceptanceCriterionRepository criteria,
      RequirementService requirementService,
      AuditService auditService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.testCases = testCases;
    this.preconditions = preconditions;
    this.steps = steps;
    this.testData = testData;
    this.reviews = reviews;
    this.revisions = revisions;
    this.links = links;
    this.criteria = criteria;
    this.requirementService = requirementService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Lists resources visible to the current owner using the requested page. */
  @Transactional(readOnly = true)
  public List<TestCaseResponse> list(UUID ownerId, UUID requirementId) {
    requirementService.requireOwned(ownerId, requirementId);
    return testCases.findAllByRequirementIdOrderByWorkItemNumber(requirementId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Returns the owned resource identified by the request. */
  @Transactional(readOnly = true)
  public TestCaseResponse get(UUID ownerId, UUID testCaseId) {
    return toResponse(requireOwned(ownerId, testCaseId));
  }

  /** Applies a validated update while preserving concurrency guarantees. */
  @Transactional
  public TestCaseResponse update(UUID ownerId, UUID testCaseId, UpdateTestCaseRequest request) {
    TestCaseEntity testCase = requireOwned(ownerId, testCaseId);
    if (testCase.getVersion() != request.version()) {
      throw ApiExceptions.conflict(
          "stale_version", "This test case changed since it was loaded. Refresh and retry.");
    }
    validateStepNumbers(request);
    RequirementEntity requirement =
        requirementService.requireOwned(ownerId, testCase.getRequirementId());
    saveRevision(testCase, ownerId);
    testCase.update(
        request.title().strip(),
        request.objective().strip(),
        request.category(),
        request.priority(),
        request.riskLevel(),
        request.automationCandidate(),
        request.rationale().strip(),
        request.finalExpectedOutcome().strip(),
        clock.instant());
    replaceParts(testCaseId, request);
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "TEST_CASE",
        testCaseId,
        "UPDATED",
        Map.of("testCaseKey", testCase.getTestCaseKey()));
    return toResponse(testCase);
  }

  /** Executes the review operation for TestCaseService. */
  @Transactional
  public TestCaseResponse review(
      UUID ownerId, UUID testCaseId, ReviewDecision decision, ReviewRequest request) {
    TestCaseEntity testCase = requireOwned(ownerId, testCaseId);
    RequirementEntity requirement =
        requirementService.requireOwned(ownerId, testCase.getRequirementId());
    testCase.review(decision, clock.instant());
    reviews.save(
        TestCaseReviewEntity.create(
            testCaseId, ownerId, decision, clean(request.comments()), clock.instant()));
    auditService.record(
        ownerId,
        requirement.getProjectId(),
        "TEST_CASE",
        testCaseId,
        "REVIEWED",
        Map.of("decision", decision.name(), "testCaseKey", testCase.getTestCaseKey()));
    return toResponse(testCase);
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
    Map<UUID, String> keyById = new HashMap<>();
    criteria
        .findAllByRequirementIdOrderBySortOrder(testCase.getRequirementId())
        .forEach(item -> keyById.put(item.getId(), item.getCriterionKey()));
    List<String> criterionKeys =
        links.findAllByTestCaseId(testCase.getId()).stream()
            .map(link -> keyById.get(link.getAcceptanceCriterionId()))
            .filter(java.util.Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    return new TestCaseResponse(
        testCase.getId(),
        testCase.getWorkItemNumber(),
        testCase.getRequirementId(),
        testCase.getGenerationRunId(),
        testCase.getTestCaseKey(),
        testCase.getTitle(),
        testCase.getObjective(),
        testCase.getCategory(),
        testCase.getPriority(),
        testCase.getRiskLevel(),
        testCase.isAutomationCandidate(),
        testCase.getStatus(),
        testCase.getCoverageIntent(),
        testCase.getRationale(),
        testCase.getFinalExpectedOutcome(),
        preconditions.findAllByTestCaseIdOrderBySortOrder(testCase.getId()).stream()
            .map(item -> new PreconditionResponse(item.getSortOrder(), item.getDescription()))
            .toList(),
        steps.findAllByTestCaseIdOrderByStepNumber(testCase.getId()).stream()
            .map(
                item ->
                    new StepResponse(
                        item.getStepNumber(),
                        item.getAction(),
                        item.getExpectedResult(),
                        item.getTestDataReference()))
            .toList(),
        testData.findAllByTestCaseIdOrderByName(testCase.getId()).stream()
            .map(
                item ->
                    new TestDataResponse(
                        item.getName(),
                        item.getDescription(),
                        item.getExampleValue(),
                        item.getSensitivity(),
                        item.getGenerationStrategy()))
            .toList(),
        criterionKeys,
        reviews.findAllByTestCaseIdOrderByCreatedAtDesc(testCase.getId()).stream()
            .map(
                item ->
                    new ReviewResponse(
                        item.getId(),
                        item.getReviewerId(),
                        item.getDecision(),
                        item.getComments(),
                        item.getCreatedAt()))
            .toList(),
        testCase.getCreatedAt(),
        testCase.getUpdatedAt(),
        testCase.getVersion());
  }

  /** Executes the replace parts operation for TestCaseService. */
  private void replaceParts(UUID testCaseId, UpdateTestCaseRequest request) {
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
    request
        .steps()
        .forEach(
            item ->
                steps.save(
                    TestStepEntity.create(
                        testCaseId,
                        item.stepNumber(),
                        item.action().strip(),
                        item.expectedResult().strip(),
                        clean(item.testDataReference()))));
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

  /** Normalizes optional text before it is compared or persisted. */
  private String clean(String value) {
    return value == null ? "" : value.strip();
  }
}
