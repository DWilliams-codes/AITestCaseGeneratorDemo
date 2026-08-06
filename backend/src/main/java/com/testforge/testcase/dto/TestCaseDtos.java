package com.testforge.testcase.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.DataSensitivity;
import com.testforge.testcase.domain.ReviewDecision;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestCaseRevisionChangeType;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.domain.TestPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestCaseDtos {
  /** Prevents instantiation because TestCaseDtos is a static utility namespace. */
  private TestCaseDtos() {}

  public record StepRequest(
      @Positive int stepNumber,
      @NotBlank @Size(max = 4000) String action,
      @NotBlank @Size(max = 4000) String expectedResult,
      @Size(max = 1000) String testDataReference) {}

  public record TestDataRequest(
      @NotBlank @Size(max = 200) String name,
      @NotBlank @Size(max = 2000) String description,
      @NotBlank @Size(max = 1000) String exampleValue,
      @NotNull DataSensitivity sensitivity,
      @NotBlank @Size(max = 100) String generationStrategy) {}

  public record UpdateTestCaseRequest(
      @NotBlank @Size(max = 300) String title,
      @NotBlank @Size(max = 4000) String objective,
      @NotNull TestCaseCategory category,
      @NotNull TestPriority priority,
      @NotNull TestPriority riskLevel,
      boolean automationCandidate,
      @NotBlank @Size(max = 4000) String rationale,
      @NotBlank @Size(max = 4000) String finalExpectedOutcome,
      @NotNull @Size(max = 30) List<@NotBlank @Size(max = 4000) String> preconditions,
      @NotNull @Size(min = 1, max = 30) List<@Valid StepRequest> steps,
      @NotNull @Size(max = 30) List<@Valid TestDataRequest> testData,
      @NotNull @PositiveOrZero Long version) {
    /** Initializes UpdateTestCaseRequest with its required collaborators and domain state. */
    public UpdateTestCaseRequest {
      preconditions = preconditions == null ? null : List.copyOf(preconditions);
      steps = steps == null ? null : List.copyOf(steps);
      testData = testData == null ? null : List.copyOf(testData);
    }
  }

  public record ReviewRequest(
      @Size(max = 4000) String comments, @NotNull @PositiveOrZero Long version) {}

  public record ReopenRequest(
      @NotBlank @Size(max = 4000) String reason, @NotNull @PositiveOrZero Long version) {}

  public record PreconditionResponse(int sortOrder, String description) {}

  public record StepResponse(
      int stepNumber, String action, String expectedResult, String testDataReference) {}

  public record TestDataResponse(
      String name,
      String description,
      String exampleValue,
      DataSensitivity sensitivity,
      String generationStrategy) {}

  public record ReviewResponse(
      UUID id, UUID reviewerId, ReviewDecision decision, String comments, Instant createdAt) {}

  public record RevisionResponse(
      UUID id,
      long revisionNumber,
      JsonNode snapshot,
      UUID changedBy,
      Instant changedAt,
      TestCaseRevisionChangeType changeType,
      String changeReason) {}

  public record TestCaseResponse(
      UUID id,
      long workItemNumber,
      UUID requirementId,
      UUID generationRunId,
      String testCaseKey,
      String title,
      String objective,
      TestCaseCategory category,
      TestPriority priority,
      TestPriority riskLevel,
      boolean automationCandidate,
      TestCaseStatus status,
      CoverageIntent coverageIntent,
      String rationale,
      String finalExpectedOutcome,
      List<PreconditionResponse> preconditions,
      List<StepResponse> steps,
      List<TestDataResponse> testData,
      List<String> acceptanceCriteriaKeys,
      List<ReviewResponse> reviews,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    /** Initializes TestCaseResponse with its required collaborators and domain state. */
    public TestCaseResponse {
      preconditions = preconditions == null ? null : List.copyOf(preconditions);
      steps = steps == null ? null : List.copyOf(steps);
      testData = testData == null ? null : List.copyOf(testData);
      acceptanceCriteriaKeys =
          acceptanceCriteriaKeys == null ? null : List.copyOf(acceptanceCriteriaKeys);
      reviews = reviews == null ? null : List.copyOf(reviews);
    }
  }
}
