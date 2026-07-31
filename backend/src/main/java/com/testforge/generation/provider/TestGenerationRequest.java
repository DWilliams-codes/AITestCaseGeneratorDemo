package com.testforge.generation.provider;

import java.util.List;
import java.util.UUID;

public record TestGenerationRequest(
    UUID requirementId,
    String title,
    String userStory,
    String businessRequirements,
    String assumptions,
    List<CriterionInput> acceptanceCriteria,
    String correlationId) {
  /** Initializes TestGenerationRequest with its required collaborators and domain state. */
  public TestGenerationRequest {
    acceptanceCriteria = acceptanceCriteria == null ? null : List.copyOf(acceptanceCriteria);
  }

  public record CriterionInput(String key, String description) {}
}
