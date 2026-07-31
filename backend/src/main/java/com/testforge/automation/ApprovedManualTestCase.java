package com.testforge.automation;

import java.util.List;
import java.util.UUID;

public record ApprovedManualTestCase(
    UUID id,
    String testCaseKey,
    String title,
    List<String> preconditions,
    List<ApprovedStep> steps,
    String finalExpectedOutcome,
    List<String> acceptanceCriteriaKeys) {
  /** Initializes ApprovedManualTestCase with its required collaborators and domain state. */
  public ApprovedManualTestCase {
    preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
    steps = steps == null ? List.of() : List.copyOf(steps);
    acceptanceCriteriaKeys =
        acceptanceCriteriaKeys == null ? List.of() : List.copyOf(acceptanceCriteriaKeys);
  }

  public record ApprovedStep(
      int stepNumber, String action, String expectedResult, String testDataReference) {}
}
