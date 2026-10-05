package com.testforge.generation.provider;

import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult.GeneratedAmbiguity;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestData;
import com.testforge.generation.provider.TestGenerationResult.RequirementSummary;
import com.testforge.generation.provider.TestGenerationResult.UsageMetadata;
import com.testforge.requirement.domain.AmbiguityCategory;
import com.testforge.requirement.domain.AmbiguitySeverity;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.DataSensitivity;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// This deterministic fixture is deliberately test-scoped so deployed runtimes cannot emit
// canned manual test cases or steps.
@Component
@ConditionalOnProperty(
    name = "testforge.generation.provider",
    havingValue = "fake",
    matchIfMissing = true)
public class FakeTestGenerationProvider implements TestGenerationProvider {
  /** Generates structured manual test coverage from the requirement input. */
  @Override
  public TestGenerationResult generate(TestGenerationRequest request) {
    List<GeneratedAmbiguity> ambiguities = detectAmbiguities(request);
    List<GeneratedTestCase> cases = new ArrayList<>();
    for (CriterionInput criterion : request.acceptanceCriteria()) {
      cases.add(directCase(request, criterion));
    }
    CriterionInput primary = request.acceptanceCriteria().getFirst();
    if (mentionsValidation(request)) {
      cases.add(validationCase(request, primary));
    }
    if (mentionsFailure(request)) {
      cases.add(errorCase(request, primary));
    }
    if (mentionsUi(request)) {
      cases.add(accessibilityCase(request, primary));
    }
    int inputTokens =
        Math.max(
            1,
            (request.title().length()
                    + request.userStory().length()
                    + request.businessRequirements().length())
                / 4);
    int outputTokens = estimateOutputTokens(cases);
    return new TestGenerationResult(
        new RequirementSummary(
            inferActor(request.userStory()),
            request.title(),
            "Provide verified behavior for " + request.title().toLowerCase(Locale.ROOT) + '.',
            request.assumptions().isBlank() ? List.of() : List.of(request.assumptions())),
        ambiguities,
        List.copyOf(cases),
        new UsageMetadata(inputTokens, outputTokens));
  }

  /** Returns the stable provider identifier stored with generation runs. */
  @Override
  public String providerName() {
    return "requirement-rules";
  }

  /** Returns the model or engine identifier stored with generation runs. */
  @Override
  public String modelName() {
    return "testforge-rules-v2";
  }

  /** Executes the direct case operation for FakeTestGenerationProvider. */
  private GeneratedTestCase directCase(TestGenerationRequest request, CriterionInput criterion) {
    return new GeneratedTestCase(
        "Confirm " + sentenceFragment(criterion.description()),
        "Verify the successful behavior described by " + criterion.key() + '.',
        TestCaseCategory.HAPPY_PATH,
        TestPriority.HIGH,
        TestPriority.HIGH,
        true,
        CoverageIntent.ACCEPTANCE_CRITERIA,
        List.of(
            "A synthetic authorized tester account is available in the designated test environment.",
            "Synthetic records required by the scenario are available."),
        List.of(syntheticData(request, criterion)),
        List.of(
            new GeneratedStep(
                1,
                "Open the designated test-environment sign-in page.",
                "The sign-in form is visible.",
                null),
            new GeneratedStep(
                2,
                "Enter the synthetic authorized tester identifier into the sign-in identifier input.",
                "The identifier input displays the synthetic tester identifier.",
                null),
            new GeneratedStep(
                3,
                "Enter the synthetic credential into the sign-in password input.",
                "The password input contains a masked synthetic credential.",
                null),
            new GeneratedStep(
                4, "Select the sign-in control once.", "The tester dashboard opens.", null),
            new GeneratedStep(
                5,
                "Inspect the tester dashboard account identity.",
                "The dashboard displays the synthetic account identity.",
                null)),
        List.of(
            new GeneratedStep(
                1,
                "Open the workflow for " + request.title() + '.',
                "The workflow is visible.",
                null),
            new GeneratedStep(
                2,
                "Enter the value from validScenarioData into the required workflow input.",
                "The required input displays the synthetic value from validScenarioData.",
                "validScenarioData"),
            new GeneratedStep(
                3,
                "Select the workflow submit control once.",
                "The requested operation is submitted once.",
                null),
            new GeneratedStep(4, "Inspect the resulting record.", criterion.description(), null),
            new GeneratedStep(
                5,
                "Inspect the confirmation state.",
                "The confirmation state is displayed.",
                null)),
        "The submitted synthetic data is stored.",
        List.of(criterion.key()),
        "Provides direct evidence for " + criterion.key() + '.');
  }

  /** Executes the validation case operation for FakeTestGenerationProvider. */
  private GeneratedTestCase validationCase(
      TestGenerationRequest request, CriterionInput criterion) {
    return new GeneratedTestCase(
        "Reject missing required input for " + request.title(),
        "Confirm that incomplete input is rejected without creating partial data.",
        TestCaseCategory.VALIDATION,
        TestPriority.HIGH,
        TestPriority.HIGH,
        true,
        CoverageIntent.ACCEPTANCE_CRITERIA,
        List.of(
            "A synthetic authorized tester account is available in the designated test environment."),
        List.of(
            new GeneratedTestData(
                "missingRequiredValue",
                "A deliberately omitted required value.",
                "<empty>",
                DataSensitivity.PUBLIC,
                "Leave the required field empty.")),
        List.of(
            new GeneratedStep(
                1,
                "Open the designated test-environment sign-in page.",
                "The sign-in form is visible.",
                null),
            new GeneratedStep(
                2,
                "Enter the synthetic authorized tester identifier into the sign-in identifier input.",
                "The identifier input displays the synthetic tester identifier.",
                null),
            new GeneratedStep(
                3,
                "Enter the synthetic credential into the sign-in password input.",
                "The password input contains a masked synthetic credential.",
                null),
            new GeneratedStep(
                4, "Select the sign-in control once.", "The tester dashboard opens.", null),
            new GeneratedStep(
                5,
                "Inspect the tester dashboard account identity.",
                "The dashboard displays the synthetic account identity.",
                null)),
        List.of(
            new GeneratedStep(
                1,
                "Open the workflow for " + request.title() + '.',
                "The workflow input controls are visible.",
                null),
            new GeneratedStep(
                2,
                "Leave the required workflow input empty.",
                "The incomplete value remains visibly unpopulated.",
                "missingRequiredValue"),
            new GeneratedStep(
                3,
                "Select the workflow submit control once.",
                "A specific validation message identifies the missing value.",
                null),
            new GeneratedStep(
                4, "Inspect the submission state.", "Submission remains blocked.", null)),
        "No record is created or changed while required input is missing.",
        List.of(criterion.key()),
        "Tests input validation and protects data integrity around " + criterion.key() + '.');
  }

  /** Executes the error case operation for FakeTestGenerationProvider. */
  private GeneratedTestCase errorCase(TestGenerationRequest request, CriterionInput criterion) {
    return new GeneratedTestCase(
        "Preserve user input when " + request.title() + " cannot be completed",
        "Verify safe, recoverable behavior when the operation is temporarily unavailable.",
        TestCaseCategory.ERROR_HANDLING,
        TestPriority.MEDIUM,
        TestPriority.HIGH,
        false,
        CoverageIntent.SUPPORTING_EXPLORATORY,
        List.of(
            "The test environment can simulate a temporary dependency failure without production data."),
        List.of(syntheticData(request, criterion)),
        List.of(
            new GeneratedStep(
                1,
                "Enable the synthetic dependency-failure condition in the test environment.",
                "The test environment reports that the dependency failure is active.",
                null)),
        List.of(
            new GeneratedStep(
                1,
                "Enter the synthetic valid data while the dependency failure is active.",
                "The entered values remain available for submission.",
                "validScenarioData"),
            new GeneratedStep(
                2,
                "Select the workflow submit control once.",
                "A non-sensitive error message is displayed.",
                null),
            new GeneratedStep(
                3,
                "Restore the synthetic dependency service.",
                "The test environment reports that the dependency service is available.",
                null),
            new GeneratedStep(
                4, "Select the workflow retry control once.", "The operation completes.", null),
            new GeneratedStep(
                5,
                "Inspect the records created by the retry.",
                "One resulting record is present.",
                null)),
        "The operation recovers after the synthetic failure.",
        List.of(criterion.key()),
        "Adds supporting recovery coverage without inventing a business-specific error rule.");
  }

  /** Executes the accessibility case operation for FakeTestGenerationProvider. */
  private GeneratedTestCase accessibilityCase(
      TestGenerationRequest request, CriterionInput criterion) {
    return new GeneratedTestCase(
        "Complete " + request.title() + " using keyboard navigation",
        "Confirm that the described user workflow is operable without a pointing device.",
        TestCaseCategory.ACCESSIBILITY,
        TestPriority.MEDIUM,
        TestPriority.MEDIUM,
        true,
        CoverageIntent.SUPPORTING_EXPLORATORY,
        List.of(
            "The browser supports standard keyboard navigation and assistive-technology test settings."),
        List.of(),
        List.of(
            new GeneratedStep(
                1,
                "Enable the synthetic keyboard-only browser test setting.",
                "The browser test setting confirms keyboard-only navigation is active.",
                null)),
        List.of(
            new GeneratedStep(
                1,
                "Press Tab to focus the first workflow control.",
                "The first workflow control shows a visible focus indicator.",
                null),
            new GeneratedStep(
                2,
                "Press Tab to move focus to the workflow submit control.",
                "The workflow submit control shows a visible focus indicator.",
                null),
            new GeneratedStep(
                3,
                "Press Enter on the focused workflow submit control.",
                "A result is announced.",
                null)),
        "The core workflow remains operable and understandable with keyboard navigation.",
        List.of(criterion.key()),
        "Provides supporting accessibility coverage for the user-facing workflow.");
  }

  /** Detects ambiguities for the current operation. */
  private List<GeneratedAmbiguity> detectAmbiguities(TestGenerationRequest request) {
    String source =
        (request.userStory() + ' ' + request.businessRequirements()).toLowerCase(Locale.ROOT);
    List<GeneratedAmbiguity> result = new ArrayList<>();
    if (!source.matches(".*\\b(role|permission|authorized|administrator|admin)\\b.*")) {
      result.add(
          new GeneratedAmbiguity(
              AmbiguityCategory.MISSING_PERMISSION_RULE,
              AmbiguitySeverity.MEDIUM,
              "The requirement does not define which roles may perform this workflow.",
              "Which roles may view, create, update, or cancel this operation?"));
    }
    if (!source.matches(".*\\b(error|invalid|failure|unavailable|reject)\\b.*")) {
      result.add(
          new GeneratedAmbiguity(
              AmbiguityCategory.MISSING_ERROR_BEHAVIOR,
              AmbiguitySeverity.MEDIUM,
              "The expected behavior for validation or dependency failures is not specified.",
              "What message, retry behavior, and data-preservation behavior are required on failure?"));
    }
    return List.copyOf(result);
  }

  /** Executes the synthetic data operation for FakeTestGenerationProvider. */
  private GeneratedTestData syntheticData(TestGenerationRequest request, CriterionInput criterion) {
    return new GeneratedTestData(
        "validScenarioData",
        "Synthetic, non-production values derived for " + criterion.key() + '.',
        request.requirementId() + "-" + criterion.key().toLowerCase(Locale.ROOT),
        DataSensitivity.PUBLIC,
        "Create a unique value for this requirement and acceptance criterion.");
  }

  /** Infers actor for the current operation. */
  private String inferActor(String story) {
    String lower = story.toLowerCase(Locale.ROOT);
    int asIndex = lower.indexOf("as a ");
    int comma = story.indexOf(',', Math.max(0, asIndex));
    if (asIndex >= 0 && comma > asIndex) {
      return story.substring(asIndex + 5, comma).strip();
    }
    return "QA-authorized user";
  }

  /** Executes the mentions ui operation for FakeTestGenerationProvider. */
  private boolean mentionsUi(TestGenerationRequest request) {
    return sourceText(request)
        .matches(".*\\b(page|screen|form|button|dialog|website|application)\\b.*");
  }

  /** Executes the mentions validation operation for FakeTestGenerationProvider. */
  private boolean mentionsValidation(TestGenerationRequest request) {
    return sourceText(request)
        .matches(".*\\b(required|invalid|reject|rejected|missing|blocked|duplicate)\\b.*");
  }

  /** Executes the mentions failure operation for FakeTestGenerationProvider. */
  private boolean mentionsFailure(TestGenerationRequest request) {
    return sourceText(request)
        .matches(".*\\b(error|fail|failed|failure|unavailable|retry|timeout)\\b.*");
  }

  /** Executes the source text operation for FakeTestGenerationProvider. */
  private String sourceText(TestGenerationRequest request) {
    return (request.userStory()
            + ' '
            + request.businessRequirements()
            + ' '
            + request.acceptanceCriteria().stream()
                .map(CriterionInput::description)
                .reduce("", (left, right) -> left + ' ' + right))
        .toLowerCase(Locale.ROOT);
  }

  /** Estimates output tokens for the current operation. */
  private int estimateOutputTokens(List<GeneratedTestCase> cases) {
    int characters =
        cases.stream()
            .mapToInt(
                testCase ->
                    length(testCase.title())
                        + length(testCase.objective())
                        + length(testCase.finalExpectedOutcome())
                        + length(testCase.rationale())
                        + testCase.preconditions().stream().mapToInt(this::length).sum()
                        + testCase.setupSteps().stream()
                            .mapToInt(
                                step ->
                                    length(step.action())
                                        + length(step.expectedResult())
                                        + length(step.testDataReference()))
                            .sum()
                        + testCase.steps().stream()
                            .mapToInt(
                                step ->
                                    length(step.action())
                                        + length(step.expectedResult())
                                        + length(step.testDataReference()))
                            .sum()
                        + testCase.testData().stream()
                            .mapToInt(
                                data ->
                                    length(data.name())
                                        + length(data.description())
                                        + length(data.exampleValue())
                                        + length(data.generationStrategy()))
                            .sum())
            .sum();
    return Math.max(1, characters / 4);
  }

  /** Executes the length operation for FakeTestGenerationProvider. */
  private int length(String value) {
    return value == null ? 0 : value.length();
  }

  /** Executes the sentence fragment operation for FakeTestGenerationProvider. */
  private String sentenceFragment(String description) {
    String trimmed = description.strip();
    if (trimmed.endsWith(".")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return Character.toLowerCase(trimmed.charAt(0)) + trimmed.substring(1);
  }
}
