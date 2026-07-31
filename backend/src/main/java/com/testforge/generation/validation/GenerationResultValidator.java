package com.testforge.generation.validation;

import com.testforge.config.GenerationProperties;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestData;
import com.testforge.testcase.domain.CoverageIntent;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class GenerationResultValidator {
  private static final List<String> VAGUE_PHRASES =
      List.of("works correctly", "behaves correctly", "as expected", "verify it works");
  private static final Pattern EXECUTABLE_CONTENT =
      Pattern.compile(
          "(?is)(```|\\bcurl\\s+https?://|\\bpowershell\\b|\\bDROP\\s+TABLE\\b|<script[ >])");

  private final GenerationProperties properties;

  public GenerationResultValidator(GenerationProperties properties) {
    this.properties = properties;
  }

  public void validate(TestGenerationRequest request, TestGenerationResult result) {
    if (result == null || result.testCases() == null || result.testCases().isEmpty()) {
      fail("The provider returned no test cases.");
    }
    if (result.testCases().size() > properties.maximumCases()) {
      fail("The provider returned more test cases than allowed.");
    }
    Set<String> allowedKeys =
        request.acceptanceCriteria().stream()
            .map(TestGenerationRequest.CriterionInput::key)
            .collect(java.util.stream.Collectors.toSet());
    Set<String> titles = new HashSet<>();
    for (GeneratedTestCase testCase : result.testCases()) {
      validateCase(testCase, allowedKeys, titles);
    }
  }

  private void validateCase(
      GeneratedTestCase testCase, Set<String> allowedKeys, Set<String> normalizedTitles) {
    requireText(testCase.title(), "A generated test case title is missing.");
    requireText(testCase.objective(), "A generated test case objective is missing.");
    requireText(testCase.finalExpectedOutcome(), "A final expected outcome is missing.");
    requireText(testCase.rationale(), "A generated test case rationale is missing.");
    if (testCase.category() == null
        || testCase.priority() == null
        || testCase.riskLevel() == null
        || testCase.coverageIntent() == null) {
      fail("A generated test case contains an unsupported enum value.");
    }
    String titleKey = testCase.title().strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    if (!normalizedTitles.add(titleKey)) {
      fail("The provider returned duplicate test cases.");
    }
    if (testCase.steps() == null
        || testCase.steps().isEmpty()
        || testCase.steps().size() > properties.maximumStepsPerCase()) {
      fail("Every generated test case must contain an allowed number of steps.");
    }
    for (int index = 0; index < testCase.steps().size(); index++) {
      GeneratedStep step = testCase.steps().get(index);
      if (step.stepNumber() != index + 1) {
        fail("Generated step numbers must be contiguous and start at one.");
      }
      requireText(step.action(), "A generated step action is missing.");
      requireText(step.expectedResult(), "A generated step expected result is missing.");
      rejectUnsafeOrVague(step.action());
      rejectUnsafeOrVague(step.expectedResult());
    }
    List<String> mappedKeys = testCase.acceptanceCriteriaKeys();
    if (testCase.coverageIntent() == CoverageIntent.ACCEPTANCE_CRITERIA
        && (mappedKeys == null || mappedKeys.isEmpty())) {
      fail("Direct test cases must map to an acceptance criterion.");
    }
    if (mappedKeys != null && !allowedKeys.containsAll(mappedKeys)) {
      fail("A generated test case references an unknown acceptance criterion.");
    }
    if (testCase.preconditions() != null) {
      testCase.preconditions().forEach(this::rejectUnsafeOrVague);
    }
    if (testCase.testData() != null) {
      for (GeneratedTestData data : testCase.testData()) {
        requireText(data.name(), "Generated test data requires a name.");
        requireText(data.description(), "Generated test data requires a description.");
        requireText(data.exampleValue(), "Generated test data requires a synthetic example.");
        if (data.sensitivity() == null) {
          fail("Generated test data sensitivity is missing.");
        }
        rejectUnsafeOrVague(data.exampleValue());
      }
    }
    rejectUnsafeOrVague(testCase.title());
    rejectUnsafeOrVague(testCase.objective());
    rejectUnsafeOrVague(testCase.finalExpectedOutcome());
  }

  private void rejectUnsafeOrVague(String value) {
    requireText(value, "Generated text must not be blank.");
    String normalized = value.toLowerCase(Locale.ROOT);
    if (VAGUE_PHRASES.stream().anyMatch(normalized::contains)) {
      fail("Generated output contains vague, untestable language.");
    }
    if (EXECUTABLE_CONTENT.matcher(value).find()) {
      fail("Generated output contains prohibited executable content.");
    }
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      fail(message);
    }
  }

  private void fail(String message) {
    throw new GenerationValidationException(message);
  }
}
