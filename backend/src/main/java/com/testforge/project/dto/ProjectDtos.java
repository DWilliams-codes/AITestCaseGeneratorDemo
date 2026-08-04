package com.testforge.project.dto;

import com.testforge.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ProjectDtos {
  /** Prevents instantiation because ProjectDtos is a static utility namespace. */
  private ProjectDtos() {}

  public record CreateProjectRequest(
      @NotBlank @Size(max = 120) String name, @Size(max = 2000) String description) {}

  public record UpdateProjectRequest(
      @NotBlank @Size(max = 120) String name,
      @Size(max = 2000) String description,
      @NotNull @PositiveOrZero Long version) {}

  public record ProjectResponse(
      UUID id,
      UUID workspaceId,
      String name,
      String description,
      ProjectStatus status,
      long requirementCount,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
