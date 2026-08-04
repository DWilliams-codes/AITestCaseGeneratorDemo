package com.testforge.workspace.application;

import com.testforge.common.error.ApiExceptions;
import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import com.testforge.workspace.domain.WorkspaceEntity;
import com.testforge.workspace.domain.WorkspaceMembershipEntity;
import com.testforge.workspace.domain.WorkspaceRole;
import com.testforge.workspace.domain.WorkspaceStatus;
import com.testforge.workspace.dto.WorkspaceDtos.WorkspaceResponse;
import com.testforge.workspace.repository.WorkspaceMembershipRepository;
import com.testforge.workspace.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
  private final WorkspaceRepository workspaces;
  private final WorkspaceMembershipRepository memberships;
  private final UserRepository users;
  private final Clock clock;

  /** Initializes workspace orchestration with persistence and time collaborators. */
  public WorkspaceService(
      WorkspaceRepository workspaces,
      WorkspaceMembershipRepository memberships,
      UserRepository users,
      Clock clock) {
    this.workspaces = workspaces;
    this.memberships = memberships;
    this.users = users;
    this.clock = clock;
  }

  /** Provisions the deterministic personal workspace and OWNER membership for a user. */
  @Transactional
  public UUID provisionPersonalWorkspace(UserEntity user, Instant now) {
    UUID userId = user.getId();
    Optional<WorkspaceEntity> existingWorkspace = workspaces.findById(userId);
    Optional<WorkspaceMembershipEntity> existingMembership =
        memberships.findByWorkspaceIdAndUserId(userId, userId);
    if (existingWorkspace.isEmpty() && existingMembership.isPresent()) {
      throw invariantViolation();
    }
    WorkspaceEntity workspace =
        existingWorkspace.orElseGet(
            () -> {
              WorkspaceEntity created =
                  WorkspaceEntity.personal(userId, user.getDisplayName(), now);
              workspaces.save(created);
              return created;
            });
    assertPersonalWorkspace(workspace, userId);
    WorkspaceMembershipEntity membership =
        existingMembership.orElseGet(
            () -> {
              WorkspaceMembershipEntity created =
                  WorkspaceMembershipEntity.personalOwner(userId, now);
              memberships.save(created);
              return created;
            });
    assertPersonalMembership(membership, userId);
    if (!workspace.getId().equals(membership.getWorkspaceId())) {
      throw invariantViolation();
    }
    return userId;
  }

  /** Returns the personal workspace, repairing an old-binary user if necessary. */
  @Transactional
  public UUID requirePersonalWorkspaceId(UUID userId) {
    UserEntity user =
        users
            .findByIdForPersonalWorkspaceReconciliation(userId)
            .orElseThrow(() -> ApiExceptions.notFound("User workspace not found."));
    return provisionPersonalWorkspace(user, clock.instant());
  }

  /** Lists only workspaces represented by the caller's membership rows. */
  @Transactional
  public List<WorkspaceResponse> list(UUID userId) {
    requirePersonalWorkspaceId(userId);
    return memberships.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Combines one scoped membership with its workspace and caller role. */
  private WorkspaceResponse toResponse(WorkspaceMembershipEntity membership) {
    WorkspaceEntity workspace =
        workspaces
            .findById(membership.getWorkspaceId())
            .orElseThrow(() -> new IllegalStateException("Workspace membership is orphaned."));
    return new WorkspaceResponse(
        workspace.getId(),
        workspace.getName(),
        workspace.getStatus(),
        membership.getRole(),
        membership.getStatus(),
        workspace.getCreatedAt(),
        workspace.getUpdatedAt(),
        workspace.getVersion());
  }

  /** Verifies that an existing deterministic workspace has not been repurposed. */
  private void assertPersonalWorkspace(WorkspaceEntity workspace, UUID userId) {
    if (!userId.equals(workspace.getId())
        || !userId.equals(workspace.getCreatedBy())
        || workspace.getStatus() != WorkspaceStatus.ACTIVE) {
      throw invariantViolation();
    }
  }

  /** Verifies identity, ownership, role, and lifecycle of the personal membership. */
  private void assertPersonalMembership(WorkspaceMembershipEntity membership, UUID userId) {
    if (!userId.equals(membership.getId())
        || !userId.equals(membership.getWorkspaceId())
        || !userId.equals(membership.getUserId())
        || !userId.equals(membership.getCreatedBy())
        || membership.getRole() != WorkspaceRole.OWNER
        || membership.getStatus() != WorkspaceStatus.ACTIVE) {
      throw invariantViolation();
    }
  }

  /** Creates the fail-closed error used for any malformed personal-tenancy row. */
  private IllegalStateException invariantViolation() {
    return new IllegalStateException("Personal workspace invariant is violated.");
  }
}
