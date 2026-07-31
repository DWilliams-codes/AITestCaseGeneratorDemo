package com.testforge.automation;

import java.util.List;
import java.util.Map;

public record AutomationContext(
    String targetPlatform,
    Map<String, String> environmentMetadata,
    Map<String, String> selectorMappings,
    List<String> reusableFunctions,
    Map<String, String> namingStandards,
    String authenticationFlow,
    String cleanupStandard) {
  public AutomationContext {
    environmentMetadata = immutable(environmentMetadata);
    selectorMappings = immutable(selectorMappings);
    reusableFunctions = reusableFunctions == null ? List.of() : List.copyOf(reusableFunctions);
    namingStandards = immutable(namingStandards);
  }

  private static Map<String, String> immutable(Map<String, String> values) {
    return values == null ? Map.of() : Map.copyOf(values);
  }

  @Override
  public Map<String, String> environmentMetadata() {
    return Map.copyOf(environmentMetadata);
  }

  @Override
  public Map<String, String> selectorMappings() {
    return Map.copyOf(selectorMappings);
  }

  @Override
  public Map<String, String> namingStandards() {
    return Map.copyOf(namingStandards);
  }
}
