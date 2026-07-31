package com.testforge.generation.provider;

import com.testforge.requirement.domain.AmbiguityCategory;
import com.testforge.requirement.domain.AmbiguitySeverity;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.DataSensitivity;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestPriority;
import java.util.List;

public record TestGenerationResult(
    RequirementSummary requirementSummary,
    List<GeneratedAmbiguity> ambiguities,
    List<GeneratedTestCase> testCases,
    UsageMetadata usage) {
  public TestGenerationResult {
    ambiguities = ambiguities == null ? null : List.copyOf(ambiguities);
    testCases = testCases == null ? null : List.copyOf(testCases);
  }

  public record RequirementSummary(
      String actor, String goal, String businessValue, List<String> assumptions) {
    public RequirementSummary {
      assumptions = assumptions == null ? null : List.copyOf(assumptions);
    }
  }

  public record GeneratedAmbiguity(
      AmbiguityCategory category,
      AmbiguitySeverity severity,
      String description,
      String suggestedQuestion) {}

  public record GeneratedTestCase(
      String title,
      String objective,
      TestCaseCategory category,
      TestPriority priority,
      TestPriority riskLevel,
      boolean automationCandidate,
      CoverageIntent coverageIntent,
      List<String> preconditions,
      List<GeneratedTestData> testData,
      List<GeneratedStep> steps,
      String finalExpectedOutcome,
      List<String> acceptanceCriteriaKeys,
      String rationale) {
    public GeneratedTestCase {
      preconditions = preconditions == null ? null : List.copyOf(preconditions);
      testData = testData == null ? null : List.copyOf(testData);
      steps = steps == null ? null : List.copyOf(steps);
      acceptanceCriteriaKeys =
          acceptanceCriteriaKeys == null ? null : List.copyOf(acceptanceCriteriaKeys);
    }
  }

  public record GeneratedTestData(
      String name,
      String description,
      String exampleValue,
      DataSensitivity sensitivity,
      String generationStrategy) {}

  public record GeneratedStep(
      int stepNumber, String action, String expectedResult, String testDataReference) {}

  public record UsageMetadata(int inputTokens, int outputTokens) {}
}
