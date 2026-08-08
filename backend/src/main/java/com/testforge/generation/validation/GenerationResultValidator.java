package com.testforge.generation.validation;

import com.testforge.config.GenerationProperties;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestData;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.validation.TestDataReferencePolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Independently enforces persistence and safety bounds after provider schema validation. */
@Component
public class GenerationResultValidator {
  public static final int MAX_CASES = 25;
  public static final int MAX_STEPS = 30;
  public static final int MAX_AMBIGUITIES = 50;
  public static final int MAX_COLLECTION = 30;
  public static final int MAX_TITLE = 300;
  public static final int MAX_TEXT = 4000;
  public static final int MAX_DATA_NAME = 200;
  public static final int MAX_DATA_DESCRIPTION = 2000;
  public static final int MAX_DATA_EXAMPLE = 1000;
  public static final int MAX_DATA_STRATEGY = 100;
  public static final int MAX_DATA_REFERENCE = 1000;
  public static final int MAX_CRITERION_KEY = 20;

  private static final List<String> VAGUE_PHRASES =
      List.of("works correctly", "behaves correctly", "as expected", "verify it works");
  private static final Pattern EXECUTABLE_CONTENT =
      Pattern.compile(
          "(?is)(```|\\b(?:bash|sh)\\s+-c\\b|\\bcmd(?:\\.exe)?\\s+/c\\b|\\b(?:powershell|pwsh)\\b|"
              + "\\brm\\s+-[a-z]*r[a-z]*f\\b|\\b(?:delete\\s+from|drop\\s+(?:table|database)|truncate\\s+table|alter\\s+table|insert\\s+into|update\\s+\\S+\\s+set)\\b|"
              + "\\b(?:curl|wget)\\s+|\\bInvoke-WebRequest\\b|\\bfetch\\s*\\(|\\bXMLHttpRequest\\b|"
              + "(?:https?|ftp|file|data|javascript):(?://)?|<(?:script|iframe|object|embed|svg|img)\\b|\\bon[a-z]+\\s*=)");

  private final GenerationProperties properties;
  private final TestDataReferencePolicy testDataReferences;

  /** Initializes GenerationResultValidator with its required collaborators and domain state. */
  public GenerationResultValidator(
      GenerationProperties properties, TestDataReferencePolicy testDataReferences) {
    this.properties = properties;
    this.testDataReferences = testDataReferences;
  }

  /** Validates every provider-authored value before any generated evidence is persisted. */
  public void validate(TestGenerationRequest request, TestGenerationResult result) {
    if (result == null) fail("The provider returned no structured result.");
    validateSummary(result.requirementSummary());
    if (result.ambiguities() == null || result.ambiguities().size() > MAX_AMBIGUITIES) {
      fail("The provider returned an invalid number of ambiguities.");
    }
    result
        .ambiguities()
        .forEach(
            ambiguity -> {
              if (ambiguity == null
                  || ambiguity.category() == null
                  || ambiguity.severity() == null) {
                fail("A generated ambiguity contains an unsupported value.");
              }
              validateText(ambiguity.description(), MAX_TEXT, "ambiguity description");
              validateText(ambiguity.suggestedQuestion(), MAX_TEXT, "ambiguity question");
            });

    if (result.testCases() == null
        || result.testCases().isEmpty()
        || result.testCases().size() > Math.min(properties.maximumCases(), MAX_CASES)) {
      fail("The provider returned an invalid number of test cases.");
    }
    Set<String> allowedKeys =
        request.acceptanceCriteria().stream()
            .map(TestGenerationRequest.CriterionInput::key)
            .peek(key -> validateText(key, MAX_CRITERION_KEY, "source criterion key"))
            .collect(Collectors.toUnmodifiableSet());
    Set<String> normalizedTitles = new HashSet<>();
    Set<String> directlyCovered = new HashSet<>();
    for (GeneratedTestCase testCase : result.testCases()) {
      validateCase(testCase, allowedKeys, normalizedTitles, directlyCovered);
    }
    if (!directlyCovered.equals(allowedKeys)) {
      fail("Every source acceptance criterion requires direct generated evidence.");
    }
  }

  /** Executes the validate summary operation for GenerationResultValidator. */
  private void validateSummary(TestGenerationResult.RequirementSummary summary) {
    if (summary == null) fail("The provider returned no requirement summary.");
    validateText(summary.actor(), MAX_TEXT, "summary actor");
    validateText(summary.goal(), MAX_TEXT, "summary goal");
    validateText(summary.businessValue(), MAX_TEXT, "summary business value");
    validateTextCollection(summary.assumptions(), MAX_COLLECTION, MAX_TEXT, "summary assumption");
  }

  /** Executes the validate case operation for GenerationResultValidator. */
  private void validateCase(
      GeneratedTestCase testCase,
      Set<String> allowedKeys,
      Set<String> normalizedTitles,
      Set<String> directlyCovered) {
    if (testCase == null) fail("A generated test case is missing.");
    validateText(testCase.title(), MAX_TITLE, "test-case title");
    validateText(testCase.objective(), MAX_TEXT, "test-case objective");
    validateText(testCase.finalExpectedOutcome(), MAX_TEXT, "final expected outcome");
    validateText(testCase.rationale(), MAX_TEXT, "test-case rationale");
    if (testCase.category() == null
        || testCase.priority() == null
        || testCase.riskLevel() == null
        || testCase.automationCandidate() == null
        || testCase.coverageIntent() == null) {
      fail("A generated test case contains an unsupported enum value.");
    }
    String titleKey = testCase.title().strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    if (!normalizedTitles.add(titleKey)) fail("The provider returned duplicate test cases.");

    validateTextCollection(testCase.preconditions(), MAX_COLLECTION, MAX_TEXT, "precondition");
    if (testCase.steps() == null
        || testCase.steps().isEmpty()
        || testCase.steps().size() > Math.min(properties.maximumStepsPerCase(), MAX_STEPS)) {
      fail("Every generated test case must contain an allowed number of steps.");
    }
    for (int index = 0; index < testCase.steps().size(); index++) {
      GeneratedStep step = testCase.steps().get(index);
      if (step == null || step.stepNumber() != index + 1) {
        fail("Generated step numbers must be contiguous and start at one.");
      }
      validateText(step.action(), MAX_TEXT, "step action");
      validateText(step.expectedResult(), MAX_TEXT, "step expected result");
      if (step.testDataReference() != null && !step.testDataReference().isBlank()) {
        validateText(step.testDataReference(), MAX_DATA_REFERENCE, "step data reference");
      }
    }

    List<String> mappedKeys = testCase.acceptanceCriteriaKeys();
    if (mappedKeys == null || mappedKeys.size() > MAX_AMBIGUITIES) {
      fail("A generated test case has an invalid number of criterion mappings.");
    }
    Set<String> uniqueMappings = new HashSet<>();
    for (String key : mappedKeys) {
      validateText(key, MAX_CRITERION_KEY, "criterion mapping");
      if (!uniqueMappings.add(key)) fail("A generated case repeats a criterion mapping.");
      if (!allowedKeys.contains(key)) {
        fail("A generated test case references an unknown acceptance criterion.");
      }
    }
    if (testCase.coverageIntent() == CoverageIntent.ACCEPTANCE_CRITERIA) {
      if (mappedKeys.isEmpty()) fail("Direct test cases must map to an acceptance criterion.");
      directlyCovered.addAll(mappedKeys);
    }

    if (testCase.testData() == null || testCase.testData().size() > MAX_COLLECTION) {
      fail("A generated test case has an invalid number of test-data items.");
    }
    for (GeneratedTestData data : testCase.testData()) {
      if (data == null || data.sensitivity() == null) {
        fail("Generated test data sensitivity is missing.");
      }
      validateText(data.name(), MAX_DATA_NAME, "test-data name");
      validateText(data.description(), MAX_DATA_DESCRIPTION, "test-data description");
      validateText(data.exampleValue(), MAX_DATA_EXAMPLE, "test-data example");
      validateText(data.generationStrategy(), MAX_DATA_STRATEGY, "test-data strategy");
    }
    try {
      testDataReferences.canonicalize(
          testCase.testData().stream().map(GeneratedTestData::name).toList(),
          testCase.steps().stream().map(GeneratedStep::testDataReference).toList());
    } catch (TestDataReferencePolicy.Violation violation) {
      fail(violation.getMessage());
    }
  }

  /** Executes the validate text collection operation for GenerationResultValidator. */
  private void validateTextCollection(
      List<String> values, int maximumItems, int maximumLength, String field) {
    if (values == null || values.size() > maximumItems) {
      fail("The provider returned an invalid " + field + " collection.");
    }
    values.forEach(value -> validateText(value, maximumLength, field));
  }

  /** Executes the validate text operation for GenerationResultValidator. */
  private void validateText(String value, int maximumLength, String field) {
    if (value == null || value.isBlank()) fail("Generated " + field + " must not be blank.");
    if (value.length() > maximumLength) fail("Generated " + field + " exceeds its size limit.");
    String normalized = value.toLowerCase(Locale.ROOT);
    if (VAGUE_PHRASES.stream().anyMatch(normalized::contains)) {
      fail("Generated output contains vague, untestable language.");
    }
    if (EXECUTABLE_CONTENT.matcher(value).find()) {
      fail("Generated output contains prohibited executable content.");
    }
  }

  /** Executes the fail operation for GenerationResultValidator. */
  private void fail(String message) {
    throw new GenerationValidationException(message);
  }
}
