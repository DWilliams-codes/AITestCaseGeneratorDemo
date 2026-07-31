package com.testforge.automation;

/** Stage 2 extension point. Stage 1 deliberately provides no implementation. */
public interface AutomationDraftGenerator {
  /** Generates structured manual test coverage from the requirement input. */
  AutomationDraft generate(ApprovedManualTestCase testCase, AutomationContext context);
}
