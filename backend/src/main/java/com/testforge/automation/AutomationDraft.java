package com.testforge.automation;

import java.util.List;
import java.util.Map;

public record AutomationDraft(
    String name,
    List<DraftAction> setupActions,
    List<DraftAction> testActions,
    List<DraftAction> cleanupActions,
    Map<String, String> parameters,
    List<String> unresolvedPlaceholders,
    double suitabilityScore) {
  public AutomationDraft {
    setupActions = setupActions == null ? List.of() : List.copyOf(setupActions);
    testActions = testActions == null ? List.of() : List.copyOf(testActions);
    cleanupActions = cleanupActions == null ? List.of() : List.copyOf(cleanupActions);
    parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    unresolvedPlaceholders =
        unresolvedPlaceholders == null ? List.of() : List.copyOf(unresolvedPlaceholders);
  }

  public record DraftAction(
      int order, String actionType, String targetPlaceholder, String value, String assertion) {}
}
