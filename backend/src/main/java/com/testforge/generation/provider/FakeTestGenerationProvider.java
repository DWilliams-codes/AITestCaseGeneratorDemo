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

@Component
@ConditionalOnProperty(
    name = "testforge.generation.provider",
    havingValue = "fake",
    matchIfMissing = true)
public class FakeTestGenerationProvider implements TestGenerationProvider {
  @Override
  public TestGenerationResult generate(TestGenerationRequest request) {
    List<GeneratedAmbiguity> ambiguities = detectAmbiguities(request);
    List<GeneratedTestCase> cases = new ArrayList<>();
    for (CriterionInput criterion : request.acceptanceCriteria()) {
      cases.add(directCase(request, criterion));
    }
    CriterionInput primary = request.acceptanceCriteria().getFirst();
    cases.add(validationCase(request, primary));
    cases.add(errorCase(request, primary));
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
    int outputTokens = cases.size() * 145;
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

  @Override
  public String providerName() {
    return "deterministic-fake";
  }

  @Override
  public String modelName() {
    return "testforge-rules-v1";
  }

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
            "The tester is authenticated in the designated test environment.",
            "Synthetic records required by the scenario are available."),
        List.of(syntheticData()),
        List.of(
            new GeneratedStep(
                1,
                "Open the workflow for " + request.title() + '.',
                "The workflow is available without an authorization or loading error.",
                null),
            new GeneratedStep(
                2,
                "Complete the workflow using the synthetic valid data set.",
                "The input is accepted and the requested operation is submitted once.",
                "validScenarioData"),
            new GeneratedStep(
                3,
                "Inspect the resulting record and confirmation state.",
                criterion.description(),
                null)),
        "The system satisfies " + criterion.key() + " and preserves the submitted test data.",
        List.of(criterion.key()),
        "Provides direct evidence for " + criterion.key() + '.');
  }

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
        List.of("The tester can access the workflow in the designated test environment."),
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
                "Open the workflow and leave one required value empty.",
                "The incomplete value remains visibly unpopulated.",
                "missingRequiredValue"),
            new GeneratedStep(
                2,
                "Submit the incomplete workflow.",
                "A specific validation message identifies the missing value and submission is blocked.",
                null)),
        "No record is created or changed while required input is missing.",
        List.of(criterion.key()),
        "Tests input validation and protects data integrity around " + criterion.key() + '.');
  }

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
        List.of(syntheticData()),
        List.of(
            new GeneratedStep(
                1,
                "Enter the synthetic valid data while the dependency failure is active.",
                "The entered values remain available for submission.",
                "validScenarioData"),
            new GeneratedStep(
                2,
                "Submit the workflow once.",
                "A non-sensitive error message explains that the operation could not be completed.",
                null),
            new GeneratedStep(
                3,
                "Restore the dependency and retry the operation once.",
                "The operation completes once without duplicate records.",
                null)),
        "The failure is recoverable, entered data is preserved, and no duplicate is created.",
        List.of(criterion.key()),
        "Adds supporting recovery coverage without inventing a business-specific error rule.");
  }

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
        List.of("The browser and assistive-technology test settings are enabled."),
        List.of(),
        List.of(
            new GeneratedStep(
                1,
                "Navigate through the workflow using only standard keyboard controls.",
                "Interactive controls receive a visible focus indicator in a logical order.",
                null),
            new GeneratedStep(
                2,
                "Complete and submit the workflow using the keyboard.",
                "All required actions can be completed and the result is announced clearly.",
                null)),
        "The core workflow remains operable and understandable with keyboard navigation.",
        List.of(criterion.key()),
        "Provides supporting accessibility coverage for the user-facing workflow.");
  }

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

  private GeneratedTestData syntheticData() {
    return new GeneratedTestData(
        "validScenarioData",
        "Synthetic, non-production values that satisfy the stated input rules.",
        "TF-DEMO-001",
        DataSensitivity.PUBLIC,
        "Generate a unique TF-DEMO identifier for each run.");
  }

  private String inferActor(String story) {
    String lower = story.toLowerCase(Locale.ROOT);
    int asIndex = lower.indexOf("as a ");
    int comma = story.indexOf(',', Math.max(0, asIndex));
    if (asIndex >= 0 && comma > asIndex) {
      return story.substring(asIndex + 5, comma).strip();
    }
    return "QA-authorized user";
  }

  private boolean mentionsUi(TestGenerationRequest request) {
    String source =
        (request.userStory() + ' ' + request.businessRequirements()).toLowerCase(Locale.ROOT);
    return source.matches(".*\\b(page|screen|form|button|dialog|website|application)\\b.*");
  }

  private String sentenceFragment(String description) {
    String trimmed = description.strip();
    if (trimmed.endsWith(".")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return Character.toLowerCase(trimmed.charAt(0)) + trimmed.substring(1);
  }
}
