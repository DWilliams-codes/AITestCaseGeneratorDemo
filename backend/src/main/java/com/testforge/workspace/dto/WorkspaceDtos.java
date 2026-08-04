package com.testforge.workspace.dto;

import com.testforge.workspace.domain.WorkspaceRole;
import com.testforge.workspace.domain.WorkspaceStatus;
import java.time.Instant;
import java.util.UUID;

public final class WorkspaceDtos {
  /** Prevents instantiation because workspace DTOs are a static contract namespace. */
  private WorkspaceDtos() {}

  public record WorkspaceResponse(
      UUID id,
      String name,
      WorkspaceStatus status,
      WorkspaceRole callerRole,
      WorkspaceStatus membershipStatus,
      Instant createdAt,
      Instant updatedAt,
      long version) {}
}
