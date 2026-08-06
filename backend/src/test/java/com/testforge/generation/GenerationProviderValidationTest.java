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
import com.testforge.testcase.validation.TestDataReferencePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerationProviderValidationTest {
  private final GenerationResultValidator validator =
      new GenerationResultValidator(
          new GenerationProperties("fake", 10, 5, java.time.Duration.ofMinutes(4)),
          new TestDataReferencePolicy());

  /**
   * Covers the fake provider generates deterministic valid and context sensitive coverage scenario.
   */
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
    assertThat(provider.providerName()).isEqualTo("requirement-rules");
    assertThat(provider.modelName()).isEqualTo("testforge-rules-v2");
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

  /**
   * Covers the fake provider derives case content and synthetic values from each requirement
   * scenario.
   */
  @Test
  void fakeProviderDerivesCaseContentAndSyntheticValuesFromEachRequirement() {
    FakeTestGenerationProvider provider = new FakeTestGenerationProvider();
    TestGenerationRequest archiveInvoice =
        new TestGenerationRequest(
            UUID.randomUUID(),
            "Archive an invoice",
            "As an accountant, I want to archive a paid invoice.",
            "",
            "A synthetic paid invoice exists.",
            List.of(new CriterionInput("AC-1", "The selected paid invoice becomes archived.")),
            "invoice-correlation");
    TestGenerationRequest scheduleReport =
        new TestGenerationRequest(
            UUID.randomUUID(),
            "Schedule a report",
            "As a manager, I want to schedule a report from the form.",
            "",
            "A synthetic report definition exists.",
            List.of(new CriterionInput("AC-1", "The selected report is scheduled once.")),
            "report-correlation");

    TestGenerationResult invoiceResult = provider.generate(archiveInvoice);
    TestGenerationResult reportResult = provider.generate(scheduleReport);

    assertThat(invoiceResult.testCases()).hasSize(1);
    assertThat(reportResult.testCases()).hasSize(2);
    assertThat(invoiceResult.testCases().getFirst().title())
        .containsIgnoringCase("selected paid invoice becomes archived");
    assertThat(reportResult.testCases().getFirst().title())
        .containsIgnoringCase("selected report is scheduled once");
    assertThat(invoiceResult.testCases().getFirst().testData().getFirst().exampleValue())
        .contains(archiveInvoice.requirementId().toString())
        .isNotEqualTo(reportResult.testCases().getFirst().testData().getFirst().exampleValue());
  }

  /** Accepts one direct case that independently maps multiple supplied acceptance criteria. */
  @Test
  void validatorAcceptsOneDirectCaseMappedToMultipleAcceptanceCriteria() {
    TestGenerationRequest request =
        new TestGenerationRequest(
            UUID.randomUUID(),
            "Create a return",
            "As a shopper, I want to create a return and avoid duplicate requests.",
            "One open return may exist per order line.",
            "A synthetic eligible order exists.",
            List.of(
                new CriterionInput("AC-1", "An eligible request creates one open return."),
                new CriterionInput("AC-2", "A second request does not create another return.")),
            "multi-criterion-correlation");
    GeneratedTestCase consolidated =
        copy(
            validCase("Create and prevent a duplicate return"),
            "Create and prevent a duplicate return",
            "Verify one workflow creates the first return and rejects the second request.",
            "One open return remains for the order line.",
            "Direct evidence for creation and duplicate prevention.",
            TestCaseCategory.DATA_INTEGRITY,
            TestPriority.HIGH,
            TestPriority.HIGH,
            CoverageIntent.ACCEPTANCE_CRITERIA,
            validSteps(),
            List.of("AC-1", "AC-2"),
            validData());

    validator.validate(request, result(List.of(consolidated)));
  }

  /** Covers the validator rejects missing oversized and duplicate case collections scenario. */
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

  /** Covers the validator rejects incomplete enums and step contracts scenario. */
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
    assertInvalid(request, result(List.of(withAutomationCandidate(null))));

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

  /** Covers the validator rejects invalid mappings unsafe language and incomplete data scenario. */
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
    assertInvalid(request, result(List.of(exploratory)));
  }

  /** Covers normalized duplicate names, dangling references, and canonical matching. */
  @Test
  void validatorEnforcesTestDataReferenceIntegrity() {
    TestGenerationRequest request =
        request("As a tester, I want a form, so that I can submit.", "", "");
    assertInvalid(
        request,
        result(
            List.of(
                withData(
                    List.of(
                        data("requestId", "First", "TF-1", DataSensitivity.PUBLIC),
                        data(" REQUESTID ", "Second", "TF-2", DataSensitivity.PUBLIC))))));

    GeneratedTestCase base = validCase("Dangling reference");
    assertInvalid(
        request,
        result(
            List.of(
                copy(
                    base,
                    base.title(),
                    base.objective(),
                    base.finalExpectedOutcome(),
                    base.rationale(),
                    base.category(),
                    base.priority(),
                    base.riskLevel(),
                    base.coverageIntent(),
                    List.of(
                        new GeneratedStep(
                            1,
                            "Submit the synthetic request.",
                            "One request is stored.",
                            "missing")),
                    base.acceptanceCriteriaKeys(),
                    base.testData()))));

    validator.validate(
        request,
        result(
            List.of(
                copy(
                    base,
                    "Canonical reference",
                    base.objective(),
                    base.finalExpectedOutcome(),
                    base.rationale(),
                    base.category(),
                    base.priority(),
                    base.riskLevel(),
                    base.coverageIntent(),
                    List.of(
                        new GeneratedStep(
                            1,
                            "Submit the synthetic request.",
                            "One request is stored.",
                            " REQUESTID ")),
                    base.acceptanceCriteriaKeys(),
                    base.testData()))));
  }

  /** Executes the request operation for GenerationProviderValidationTest. */
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

  /** Executes the result operation for GenerationProviderValidationTest. */
  private TestGenerationResult result(List<GeneratedTestCase> cases) {
    return new TestGenerationResult(
        new RequirementSummary("tester", "goal", "value", List.of()),
        List.of(),
        cases,
        new UsageMetadata(10, 20));
  }

  /** Executes the valid case operation for GenerationProviderValidationTest. */
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

  /** Executes the copy operation for GenerationProviderValidationTest. */
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

  /** Executes the with steps operation for GenerationProviderValidationTest. */
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

  /** Executes the with mappings operation for GenerationProviderValidationTest. */
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

  /** Executes the with title operation for GenerationProviderValidationTest. */
  private GeneratedTestCase withTitle(String title) {
    GeneratedTestCase base = validCase(title);
    return base;
  }

  /** Executes the with preconditions operation for GenerationProviderValidationTest. */
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

  /** Returns a case with an explicit automation-candidate presence value. */
  private GeneratedTestCase withAutomationCandidate(Boolean automationCandidate) {
    GeneratedTestCase base = validCase("Automation candidate contract");
    return new GeneratedTestCase(
        base.title(),
        base.objective(),
        base.category(),
        base.priority(),
        base.riskLevel(),
        automationCandidate,
        base.coverageIntent(),
        base.preconditions(),
        base.testData(),
        base.steps(),
        base.finalExpectedOutcome(),
        base.acceptanceCriteriaKeys(),
        base.rationale());
  }

  /** Executes the with data operation for GenerationProviderValidationTest. */
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

  /** Executes the valid steps operation for GenerationProviderValidationTest. */
  private List<GeneratedStep> validSteps() {
    return List.of(step(1, "Submit the synthetic request.", "One request is stored."));
  }

  /** Executes the step operation for GenerationProviderValidationTest. */
  private GeneratedStep step(int number, String action, String result) {
    return new GeneratedStep(number, action, result, null);
  }

  /** Executes the valid data operation for GenerationProviderValidationTest. */
  private List<GeneratedTestData> validData() {
    return List.of(data("requestId", "A synthetic identifier.", "TF-001", DataSensitivity.PUBLIC));
  }

  /** Executes the data operation for GenerationProviderValidationTest. */
  private GeneratedTestData data(
      String name, String description, String value, DataSensitivity sensitivity) {
    return new GeneratedTestData(name, description, value, sensitivity, "Generate a unique value.");
  }

  /** Asserts invalid for the current operation. */
  private void assertInvalid(TestGenerationRequest request, TestGenerationResult result) {
    assertThatThrownBy(() -> validator.validate(request, result))
        .isInstanceOf(GenerationValidationException.class);
  }
}
