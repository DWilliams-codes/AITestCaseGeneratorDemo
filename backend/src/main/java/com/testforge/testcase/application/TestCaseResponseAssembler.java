package com.testforge.testcase.application;

import com.testforge.generation.domain.GenerationCriterionSnapshotEntity;
import com.testforge.generation.repository.GenerationCriterionSnapshotRepository;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCasePreconditionEntity;
import com.testforge.testcase.domain.TestCaseReviewEntity;
import com.testforge.testcase.domain.TestDataItemEntity;
import com.testforge.testcase.domain.TestStepEntity;
import com.testforge.testcase.dto.TestCaseDtos.PreconditionResponse;
import com.testforge.testcase.dto.TestCaseDtos.ReviewResponse;
import com.testforge.testcase.dto.TestCaseDtos.StepResponse;
import com.testforge.testcase.dto.TestCaseDtos.TestCaseResponse;
import com.testforge.testcase.dto.TestCaseDtos.TestDataResponse;
import com.testforge.testcase.repository.TestCasePreconditionRepository;
import com.testforge.testcase.repository.TestCaseReviewRepository;
import com.testforge.testcase.repository.TestDataItemRepository;
import com.testforge.testcase.repository.TestStepRepository;
import com.testforge.traceability.domain.SnapshotTraceabilityLinkEntity;
import com.testforge.traceability.repository.SnapshotTraceabilityLinkRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Assembles complete test-case responses with a constant number of bounded batch reads. */
@Component
public class TestCaseResponseAssembler {
  private static final int EMBEDDED_REVIEW_LIMIT = 20;

  private final TestCasePreconditionRepository preconditions;
  private final TestStepRepository steps;
  private final TestDataItemRepository testData;
  private final TestCaseReviewRepository reviews;
  private final SnapshotTraceabilityLinkRepository snapshotLinks;
  private final GenerationCriterionSnapshotRepository criterionSnapshots;

  /** Initializes the assembler with the stores that own structured case evidence. */
  public TestCaseResponseAssembler(
      TestCasePreconditionRepository preconditions,
      TestStepRepository steps,
      TestDataItemRepository testData,
      TestCaseReviewRepository reviews,
      SnapshotTraceabilityLinkRepository snapshotLinks,
      GenerationCriterionSnapshotRepository criterionSnapshots) {
    this.preconditions = preconditions;
    this.steps = steps;
    this.testData = testData;
    this.reviews = reviews;
    this.snapshotLinks = snapshotLinks;
    this.criterionSnapshots = criterionSnapshots;
  }

  /** Assembles one response through the same evidence path used by collection reads. */
  public TestCaseResponse assemble(TestCaseEntity testCase) {
    return assembleAll(List.of(testCase)).getFirst();
  }

  /** Assembles a bounded page without issuing repository calls per test case. */
  public List<TestCaseResponse> assembleAll(List<TestCaseEntity> testCases) {
    if (testCases.isEmpty()) {
      return List.of();
    }
    List<UUID> testCaseIds = testCases.stream().map(TestCaseEntity::getId).toList();
    List<UUID> runIds =
        testCases.stream().map(TestCaseEntity::getGenerationRunId).distinct().toList();
    Map<UUID, List<TestCasePreconditionEntity>> preconditionsByCase =
        preconditions.findAllByTestCaseIdInOrderByTestCaseIdAscSortOrderAsc(testCaseIds).stream()
            .collect(Collectors.groupingBy(TestCasePreconditionEntity::getTestCaseId));
    Map<UUID, List<TestStepEntity>> stepsByCase =
        steps.findAllByTestCaseIdInOrderByTestCaseIdAscStepNumberAsc(testCaseIds).stream()
            .collect(Collectors.groupingBy(TestStepEntity::getTestCaseId));
    Map<UUID, List<TestDataItemEntity>> testDataByCase =
        testData.findAllByTestCaseIdInOrderByTestCaseIdAscNameAsc(testCaseIds).stream()
            .collect(Collectors.groupingBy(TestDataItemEntity::getTestCaseId));
    Map<UUID, List<TestCaseReviewEntity>> reviewsByCase =
        reviews.findRecentByTestCaseIds(testCaseIds, EMBEDDED_REVIEW_LIMIT).stream()
            .collect(Collectors.groupingBy(TestCaseReviewEntity::getTestCaseId));
    Map<Long, GenerationCriterionSnapshotEntity> snapshotsById =
        criterionSnapshots.findAllByGenerationRunIdIn(runIds).stream()
            .collect(
                Collectors.toMap(
                    GenerationCriterionSnapshotEntity::getId,
                    Function.identity(),
                    (left, right) -> left));
    Map<UUID, List<SnapshotTraceabilityLinkEntity>> linksByCase =
        snapshotLinks.findAllByTestCaseIdIn(testCaseIds).stream()
            .collect(Collectors.groupingBy(SnapshotTraceabilityLinkEntity::getTestCaseId));

    return testCases.stream()
        .map(
            testCase ->
                assemble(
                    testCase,
                    preconditionsByCase.getOrDefault(testCase.getId(), List.of()),
                    stepsByCase.getOrDefault(testCase.getId(), List.of()),
                    testDataByCase.getOrDefault(testCase.getId(), List.of()),
                    reviewsByCase.getOrDefault(testCase.getId(), List.of()),
                    linksByCase.getOrDefault(testCase.getId(), List.of()),
                    snapshotsById))
        .toList();
  }

  /** Maps one entity and its already-loaded evidence into the public response contract. */
  private TestCaseResponse assemble(
      TestCaseEntity testCase,
      List<TestCasePreconditionEntity> casePreconditions,
      List<TestStepEntity> caseSteps,
      List<TestDataItemEntity> caseTestData,
      List<TestCaseReviewEntity> caseReviews,
      List<SnapshotTraceabilityLinkEntity> caseLinks,
      Map<Long, GenerationCriterionSnapshotEntity> snapshotsById) {
    List<String> criterionKeys =
        caseLinks.stream()
            .map(link -> snapshotsById.get(link.getCriterionSnapshotId()))
            .filter(java.util.Objects::nonNull)
            .map(GenerationCriterionSnapshotEntity::getCriterionKey)
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
        casePreconditions.stream()
            .map(item -> new PreconditionResponse(item.getSortOrder(), item.getDescription()))
            .toList(),
        caseSteps.stream()
            .map(
                item ->
                    new StepResponse(
                        item.getStepNumber(),
                        item.getAction(),
                        item.getExpectedResult(),
                        item.getTestDataReference()))
            .toList(),
        caseTestData.stream()
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
        caseReviews.stream()
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
}
