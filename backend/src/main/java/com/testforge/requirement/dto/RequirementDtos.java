package com.testforge.requirement.dto;

import com.testforge.requirement.domain.AmbiguityCategory;
import com.testforge.requirement.domain.AmbiguitySeverity;
import com.testforge.requirement.domain.RequirementStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RequirementDtos {
  private RequirementDtos() {}

  public record CreateRequirementRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 10000) String userStory,
      @Size(max = 20000) String businessRequirements,
      @Size(max = 10000) String assumptions,
      @Size(max = 1000) String sourceReference,
      @NotNull @Size(min = 1, max = 50) List<@NotBlank @Size(max = 4000) String> acceptanceCriteria) {
    public CreateRequirementRequest {
      acceptanceCriteria = acceptanceCriteria == null ? null : List.copyOf(acceptanceCriteria);
    }
  }

  public record UpdateRequirementRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 10000) String userStory,
      @Size(max = 20000) String businessRequirements,
      @Size(max = 10000) String assumptions,
      @Size(max = 1000) String sourceReference,
      @NotNull RequirementStatus status,
      @NotNull @PositiveOrZero Long version) {}

  public record AcceptanceCriterionRequest(
      @NotBlank @Size(max = 20) @Pattern(regexp = "(?i)AC-[1-9][0-9]*") String criterionKey,
      @NotBlank @Size(max = 4000) String description,
      @PositiveOrZero int sortOrder) {}

  public record AcceptanceCriterionResponse(
      UUID id,
      String criterionKey,
      String description,
      int sortOrder,
      Instant createdAt,
      Instant updatedAt) {}

  public record AmbiguityResponse(
      UUID id,
      AmbiguityCategory category,
      AmbiguitySeverity severity,
      String description,
      String suggestedQuestion,
      boolean resolved,
      String resolution,
      Instant createdAt,
      Instant resolvedAt,
      long version) {}

  public record ResolveAmbiguityRequest(
      @NotBlank @Size(max = 4000) String resolution, @NotNull @PositiveOrZero Long version) {}

  public record RequirementSummaryResponse(
      UUID id,
      long workItemNumber,
      UUID projectId,
      String title,
      RequirementStatus status,
      int acceptanceCriteriaCount,
      Instant updatedAt,
      long version) {}

  public record RequirementResponse(
      UUID id,
      long workItemNumber,
      UUID projectId,
      String title,
      String userStory,
      String businessRequirements,
      String assumptions,
      String sourceReference,
      RequirementStatus status,
      List<@Valid AcceptanceCriterionResponse> acceptanceCriteria,
      List<@Valid AmbiguityResponse> ambiguities,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    public RequirementResponse {
      acceptanceCriteria = acceptanceCriteria == null ? null : List.copyOf(acceptanceCriteria);
      ambiguities = ambiguities == null ? null : List.copyOf(ambiguities);
    }
  }
}
