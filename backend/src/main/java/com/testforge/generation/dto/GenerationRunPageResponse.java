package com.testforge.generation.dto;

import java.util.List;
import java.util.UUID;

/** A bounded run-history page plus stable active-set identity independent of page contents. */
public record GenerationRunPageResponse(
    List<GenerationRunResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    UUID activeGenerationRunId) {
  /** Prevents callers from mutating the response collection after construction. */
  public GenerationRunPageResponse {
    items = List.copyOf(items);
  }
}
