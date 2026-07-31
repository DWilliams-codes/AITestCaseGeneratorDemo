package com.testforge.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.testforge.config.GenerationProperties;
import com.testforge.generation.provider.FakeTestGenerationProvider;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestData;
import com.testforge.generation.provider.TestGenerationResult.RequirementSummary;
import com.testforge.generation.provider.TestGenerationResult.UsageMetadata;
import com.testforge.generation.validation.GenerationResultValidator;
import com.testforge.generation.validation.GenerationValidationException;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.DataSensitivity;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerationProviderValidationTest {
  private final GenerationResultValidator validator =
      new GenerationResultValidator(new GenerationProperties("fake", 10, 5));

  @Test
  void fakeProviderGeneratesDeterministicValidAndContextSensitiveCoverage() {
    FakeTestGenerationProvider provider = new FakeTestGenerationProvider();
    TestGenerationRequest ambiguousUi =
        request(
            "As a customer, I want to submit the form, so that my request is recorded.",
            "The page records a request.",
            "A synthetic account exists.");

    TestGenerationResult first = provider.generate(ambiguousUi);
    TestGenerationResult second = provider.generate(ambiguousUi);

    assertThat(first).isEqualTo(second);
    assertThat(first.ambiguities()).hasSize(2);
    assertThat(first.testCases())
        .extracting(GeneratedTestCase::category)
        .contains(TestCaseCategory.ACCESSIBILITY);
    assertThat(provider.providerName()).isEqualTo("deterministic-fake");
    assertThat(provider.modelName()).isEqualTo("testforge-rules-v1");
    validator.validate(ambiguousUi, first);

    TestGenerationRequest explicitRules =
        request(
            "Create a batch request",
            "Only the administrator role is authorized. Invalid input is rejected with an error.",
            "");
    TestGenerationResult explicitResult = provider.generate(explicitRules);
    assertThat(explicitResult.ambiguities()).isEmpty();
    assertThat(explicitResult.testCases())
        .extracting(GeneratedTestCase::category)
        .doesNotContain(TestCaseCategory.ACCESSIBILITY);
    assertThat(explicitResult.requirementSummary().actor()).isEqualTo("QA-authorized user");
    assertThat(explicitResult.requirementSummary().assumptions()).isEmpty();
    validator.validate(explicitRules, explicitResult);
  }

  @Test
  void validatorRejectsMissingOversizedAndDuplicateCaseCollections() {
    TestGenerationRequest request =
        request("As a tester, I want a form, so that I can submit.", "", "");
    assertInvalid(request, null);
    assertInvalid(request, result(null));
    assertInvalid(request, result(List.of()));

    List<GeneratedTestCase> oversized = new ArrayList<>();
    for (int index = 0; index < 11; index++) {
      oversized.add(validCase("Unique case " + index));
    }
    assertInvalid(request, result(oversized));
    assertInvalid(request, result(List.of(validCase("Duplicate"), validCase(" duplicate "))));
  }

  @Test
  void validatorRejectsIncompleteEnumsAndStepContracts() {
    TestGenerationRequest request =
        request("As a tester, I want a form, so that I can submit.", "", "");
    assertInvalid(
        request,
        result(
            List.of(
                copy(
                    validCase("Title"),
                    null,
                    "Objective",
                    "Outcome",
                    "Rationale",
                    TestCaseCategory.HAPPY_PATH,
                    TestPriority.HIGH,
                    TestPriority.HIGH,
                    CoverageIntent.ACCEPTANCE_CRITERIA,
                    validSteps(),
                    List.of("AC-1"),
                    validData()))));
    assertInvalid(
        request,
        result(
            List.of(
                copy(
                    validCase("Title"),
                    "Title",
                    " ",
                    "Outcome",
                    "Rationale",
                    TestCaseCategory.HAPPY_PATH,
                    TestPriority.HIGH,
                    TestPriority.HIGH,
                    CoverageIntent.ACCEPTANCE_CRITERIA,
                    validSteps(),
                    List.of("AC-1"),
                    validData()))));
    assertInvalid(
        request,
        result(
            List.of(
                copy(
                    validCase("Title"),
                    "Title",
                    "Objective",
                    "",
                    "Rationale",
                    TestCaseCategory.HAPPY_PATH,
                    TestPriority.HIGH,
                    TestPriority.HIGH,
                    CoverageIntent.ACCEPTANCE_CRITERIA,
                    validSteps(),
                    List.of("AC-1"),
                    validData()))));
    assertInvalid(
        request,
        result(
            List.of(
                copy(
                    validCase("Title"),
                    "Title",
                    "Objective",
                    "Outcome",
                    null,
                    TestCaseCategory.HAPPY_PATH,
                    TestPriority.HIGH,
                    TestPriority.HIGH,
                    CoverageIntent.ACCEPTANCE_CRITERIA,
                    validSteps(),
                    List.of("AC-1"),
                    validData()))));

    List<GeneratedTestCase> invalidEnums =
        List.of(
            copy(
                validCase("Category"),
                "Category",
                "Objective",
                "Outcome",
                "Rationale",
                null,
                TestPriority.HIGH,
                TestPriority.HIGH,
                CoverageIntent.ACCEPTANCE_CRITERIA,
                validSteps(),
                List.of("AC-1"),
                validData()),
            copy(
                validCase("Priority"),
                "Priority",
                "Objective",
                "Outcome",
                "Rationale",
                TestCaseCategory.HAPPY_PATH,
                null,
                TestPriority.HIGH,
                CoverageIntent.ACCEPTANCE_CRITERIA,
                validSteps(),
                List.of("AC-1"),
                validData()),
            copy(
                validCase("Risk"),
                "Risk",
                "Objective",
                "Outcome",
                "Rationale",
                TestCaseCategory.HAPPY_PATH,
                TestPriority.HIGH,
                null,
                CoverageIntent.ACCEPTANCE_CRITERIA,
                validSteps(),
                List.of("AC-1"),
                validData()),
            copy(
                validCase("Intent"),
                "Intent",
                "Objective",
                "Outcome",
                "Rationale",
                TestCaseCategory.HAPPY_PATH,
                TestPriority.HIGH,
                TestPriority.HIGH,
                null,
                validSteps(),
                List.of("AC-1"),
                validData()));
    invalidEnums.forEach(testCase -> assertInvalid(request, result(List.of(testCase))));

    assertInvalid(request, result(List.of(withSteps(null))));
    assertInvalid(request, result(List.of(withSteps(List.of()))));
    assertInvalid(
        request,
        result(
            List.of(
                withSteps(
                    List.of(
                        step(1, "One", "One"),
                        step(2, "Two", "Two"),
                        step(3, "Three", "Three"),
                        step(4, "Four", "Four"),
                        step(5, "Five", "Five"),
                        step(6, "Six", "Six"))))));
    assertInvalid(request, result(List.of(withSteps(List.of(step(2, "Action", "Result"))))));
    assertInvalid(request, result(List.of(withSteps(List.of(step(1, " ", "Result"))))));
    assertInvalid(request, result(List.of(withSteps(List.of(step(1, "Action", " "))))));
  }

  @Test
  void validatorRejectsInvalidMappingsUnsafeLanguageAndIncompleteData() {
    TestGenerationRequest request =
        request("As a tester, I want a form, so that I can submit.", "", "");
    assertInvalid(request, result(List.of(withMappings(null))));
    assertInvalid(request, result(List.of(withMappings(List.of()))));
    assertInvalid(request, result(List.of(withMappings(List.of("AC-99")))));
    assertInvalid(request, result(List.of(withTitle("Verify it works correctly"))));
    assertInvalid(request, result(List.of(withTitle("Run curl https://malicious.invalid"))));
    assertInvalid(
        request, result(List.of(withPreconditions(List.of("<script>alert(1)</script>")))));

    assertInvalid(
        request,
        result(
            List.of(withData(List.of(data("", "Description", "TF-1", DataSensitivity.PUBLIC))))));
    assertInvalid(
        request,
        result(List.of(withData(List.of(data("Name", "", "TF-1", DataSensitivity.PUBLIC))))));
    assertInvalid(
        request,
        result(
            List.of(withData(List.of(data("Name", "Description", "", DataSensitivity.PUBLIC))))));
    assertInvalid(
        request, result(List.of(withData(List.of(data("Name", "Description", "TF-1", null))))));
    assertInvalid(
        request,
        result(
            List.of(
                withData(
                    List.of(data("Name", "Description", "as expected", DataSensitivity.PUBLIC))))));

    GeneratedTestCase exploratory =
        copy(
            validCase("Exploratory"),
            "Exploratory",
            "Objective",
            "Outcome",
            "Rationale",
            TestCaseCategory.ERROR_HANDLING,
            TestPriority.MEDIUM,
            TestPriority.HIGH,
            CoverageIntent.SUPPORTING_EXPLORATORY,
            validSteps(),
            null,
            null);
    validator.validate(request, result(List.of(exploratory)));
  }

  private TestGenerationRequest request(String story, String rules, String assumptions) {
    return new TestGenerationRequest(
        UUID.randomUUID(),
        "Submit a request",
        story,
        rules,
        assumptions,
        List.of(new CriterionInput("AC-1", "A request is stored once.")),
        "correlation-id");
  }

  private TestGenerationResult result(List<GeneratedTestCase> cases) {
    return new TestGenerationResult(
        new RequirementSummary("tester", "goal", "value", List.of()),
        List.of(),
        cases,
        new UsageMetadata(10, 20));
  }

  private GeneratedTestCase validCase(String title) {
    return new GeneratedTestCase(
        title,
        "Verify a concrete outcome.",
        TestCaseCategory.HAPPY_PATH,
        TestPriority.HIGH,
        TestPriority.HIGH,
        true,
        CoverageIntent.ACCEPTANCE_CRITERIA,
        List.of("A synthetic account exists."),
        validData(),
        validSteps(),
        "The request is stored exactly once.",
        List.of("AC-1"),
        "Direct evidence for AC-1.");
  }

  private GeneratedTestCase copy(
      GeneratedTestCase ignored,
      String title,
      String objective,
      String outcome,
      String rationale,
      TestCaseCategory category,
      TestPriority priority,
      TestPriority risk,
      CoverageIntent intent,
      List<GeneratedStep> steps,
      List<String> keys,
      List<GeneratedTestData> data) {
    return new GeneratedTestCase(
        title,
        objective,
        category,
        priority,
        risk,
        true,
        intent,
        List.of("A synthetic account exists."),
        data,
        steps,
        outcome,
        keys,
        rationale);
  }

  private GeneratedTestCase withSteps(List<GeneratedStep> steps) {
    GeneratedTestCase base = validCase("Step contract");
    return copy(
        base,
        base.title(),
        base.objective(),
        base.finalExpectedOutcome(),
        base.rationale(),
        base.category(),
        base.priority(),
        base.riskLevel(),
        base.coverageIntent(),
        steps,
        base.acceptanceCriteriaKeys(),
        base.testData());
  }

  private GeneratedTestCase withMappings(List<String> keys) {
    GeneratedTestCase base = validCase("Mapping contract");
    return copy(
        base,
        base.title(),
        base.objective(),
        base.finalExpectedOutcome(),
        base.rationale(),
        base.category(),
        base.priority(),
        base.riskLevel(),
        base.coverageIntent(),
        base.steps(),
        keys,
        base.testData());
  }

  private GeneratedTestCase withTitle(String title) {
    GeneratedTestCase base = validCase(title);
    return base;
  }

  private GeneratedTestCase withPreconditions(List<String> preconditions) {
    GeneratedTestCase base = validCase("Unsafe precondition");
    return new GeneratedTestCase(
        base.title(),
        base.objective(),
        base.category(),
        base.priority(),
        base.riskLevel(),
        true,
        base.coverageIntent(),
        preconditions,
        base.testData(),
        base.steps(),
        base.finalExpectedOutcome(),
        base.acceptanceCriteriaKeys(),
        base.rationale());
  }

  private GeneratedTestCase withData(List<GeneratedTestData> data) {
    GeneratedTestCase base = validCase("Data contract");
    return copy(
        base,
        base.title(),
        base.objective(),
        base.finalExpectedOutcome(),
        base.rationale(),
        base.category(),
        base.priority(),
        base.riskLevel(),
        base.coverageIntent(),
        base.steps(),
        base.acceptanceCriteriaKeys(),
        data);
  }

  private List<GeneratedStep> validSteps() {
    return List.of(step(1, "Submit the synthetic request.", "One request is stored."));
  }

  private GeneratedStep step(int number, String action, String result) {
    return new GeneratedStep(number, action, result, null);
  }

  private List<GeneratedTestData> validData() {
    return List.of(data("requestId", "A synthetic identifier.", "TF-001", DataSensitivity.PUBLIC));
  }

  private GeneratedTestData data(
      String name, String description, String value, DataSensitivity sensitivity) {
    return new GeneratedTestData(name, description, value, sensitivity, "Generate a unique value.");
  }

  private void assertInvalid(TestGenerationRequest request, TestGenerationResult result) {
    assertThatThrownBy(() -> validator.validate(request, result))
        .isInstanceOf(GenerationValidationException.class);
  }
}
