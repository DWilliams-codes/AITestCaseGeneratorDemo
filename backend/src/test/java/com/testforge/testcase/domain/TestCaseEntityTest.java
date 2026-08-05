package com.testforge.testcase.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestCaseEntityTest {
  private static final Instant CREATED_AT = Instant.parse("2026-08-04T12:00:00Z");
  private static final Instant CHANGED_AT = Instant.parse("2026-08-04T12:01:00Z");

  /** Exercises approved-state guards and the explicit reopen path. */
  @Test
  void protectsApprovedCasesUntilTheyAreReopened() {
    TestCaseEntity testCase = newCase();

    testCase.update(
        "Updated title",
        "Updated objective",
        TestCaseCategory.VALIDATION,
        TestPriority.CRITICAL,
        TestPriority.HIGH,
        true,
        "Updated rationale",
        "Updated outcome",
        true,
        CHANGED_AT);
    testCase.review(ReviewDecision.APPROVED, null, CHANGED_AT);

    assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.APPROVED);
    assertThatThrownBy(
            () ->
                testCase.update(
                    "Another title",
                    "Updated objective",
                    TestCaseCategory.VALIDATION,
                    TestPriority.CRITICAL,
                    TestPriority.HIGH,
                    true,
                    "Updated rationale",
                    "Updated outcome",
                    true,
                    CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("reopened");
    assertThatThrownBy(() -> testCase.review(ReviewDecision.APPROVED, null, CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("cannot be reviewed");
    assertThatThrownBy(() -> testCase.reopen(" ", CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("requires a reason");

    testCase.reopen("New product evidence", CHANGED_AT);

    assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.IN_REVIEW);
    assertThatThrownBy(() -> testCase.reopen("Already open", CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("Only approved or rejected");
  }

  /** Exercises rejection and requested-change comment and edit requirements. */
  @Test
  void requiresCommentsAndActualEditsForNonApprovalDecisions() {
    TestCaseEntity rejected = newCase();
    assertThatThrownBy(() -> rejected.review(ReviewDecision.REJECTED, null, CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("requires a comment");
    rejected.review(ReviewDecision.REJECTED, "Duplicate workflow", CHANGED_AT);
    assertThat(rejected.getStatus()).isEqualTo(TestCaseStatus.REJECTED);
    rejected.reopen("Corrected requirement", CHANGED_AT);
    assertThat(rejected.getStatus()).isEqualTo(TestCaseStatus.IN_REVIEW);

    TestCaseEntity revision = newCase();
    assertThatThrownBy(() -> revision.review(ReviewDecision.CHANGES_REQUESTED, "\t", CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("requires a comment");
    revision.review(ReviewDecision.CHANGES_REQUESTED, "Clarify the outcome", CHANGED_AT);
    assertThat(revision.getStatus()).isEqualTo(TestCaseStatus.NEEDS_REVISION);
    assertThatThrownBy(
            () ->
                revision.update(
                    revision.getTitle(),
                    revision.getObjective(),
                    revision.getCategory(),
                    revision.getPriority(),
                    revision.getRiskLevel(),
                    revision.isAutomationCandidate(),
                    revision.getRationale(),
                    revision.getFinalExpectedOutcome(),
                    false,
                    CHANGED_AT))
        .isInstanceOf(TestCaseEntity.InvalidTransition.class)
        .hasMessageContaining("actual edit");

    revision.update(
        "Clarified title",
        revision.getObjective(),
        revision.getCategory(),
        revision.getPriority(),
        revision.getRiskLevel(),
        revision.isAutomationCandidate(),
        revision.getRationale(),
        revision.getFinalExpectedOutcome(),
        true,
        CHANGED_AT);

    assertThat(revision.getStatus()).isEqualTo(TestCaseStatus.IN_REVIEW);
  }

  /** Creates a deterministic synthetic test case for state-machine checks. */
  private TestCaseEntity newCase() {
    return TestCaseEntity.create(
        1001,
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        "TC-1001",
        "Original title",
        "Original objective",
        TestCaseCategory.HAPPY_PATH,
        TestPriority.MEDIUM,
        TestPriority.MEDIUM,
        false,
        CoverageIntent.ACCEPTANCE_CRITERIA,
        "Original rationale",
        "Original outcome",
        UUID.fromString("00000000-0000-0000-0000-000000000003"),
        CREATED_AT);
  }
}
