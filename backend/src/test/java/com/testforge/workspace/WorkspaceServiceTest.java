package com.testforge.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import com.testforge.workspace.application.WorkspaceService;
import com.testforge.workspace.domain.WorkspaceEntity;
import com.testforge.workspace.domain.WorkspaceMembershipEntity;
import com.testforge.workspace.domain.WorkspaceRole;
import com.testforge.workspace.dto.WorkspaceDtos.WorkspaceResponse;
import com.testforge.workspace.repository.WorkspaceMembershipRepository;
import com.testforge.workspace.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkspaceServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");
  private WorkspaceRepository workspaces;
  private WorkspaceMembershipRepository memberships;
  private UserRepository users;
  private WorkspaceService service;

  /** Creates isolated persistence collaborators for each workspace-service scenario. */
  @BeforeEach
  void setUp() {
    workspaces = mock(WorkspaceRepository.class);
    memberships = mock(WorkspaceMembershipRepository.class);
    users = mock(UserRepository.class);
    service =
        new WorkspaceService(workspaces, memberships, users, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  /** Provisions a deterministic personal workspace and OWNER membership exactly once. */
  @Test
  void provisionsDeterministicPersonalWorkspaceAndOwnerMembership() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    when(user.getId()).thenReturn(userId);
    when(user.getDisplayName()).thenReturn("Quality Owner");

    UUID workspaceId = service.provisionPersonalWorkspace(user, NOW);

    assertThat(workspaceId).isEqualTo(userId);
    verify(workspaces)
        .save(org.mockito.ArgumentMatchers.argThat(entity -> userId.equals(entity.getId())));
    verify(memberships)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                entity ->
                    userId.equals(entity.getWorkspaceId())
                        && userId.equals(entity.getUserId())
                        && entity.getRole() == WorkspaceRole.OWNER));
  }

  /** Leaves an already-provisioned personal workspace unchanged. */
  @Test
  void treatsPersonalWorkspaceProvisioningAsIdempotent() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    WorkspaceEntity workspace = WorkspaceEntity.personal(userId, "Quality Owner", NOW);
    WorkspaceMembershipEntity membership =
        WorkspaceMembershipEntity.personalOwner(userId, NOW);
    when(user.getId()).thenReturn(userId);
    when(workspaces.findById(userId)).thenReturn(Optional.of(workspace));
    when(memberships.findByWorkspaceIdAndUserId(userId, userId))
        .thenReturn(Optional.of(membership));

    assertThat(service.provisionPersonalWorkspace(user, NOW)).isEqualTo(userId);

    verify(workspaces, never()).save(org.mockito.ArgumentMatchers.any());
    verify(memberships, never()).save(org.mockito.ArgumentMatchers.any());
  }

  /** Repairs a user created by an older application binary before returning its workspace. */
  @Test
  void repairsMissingPersonalWorkspaceDuringProjectDualWrite() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    when(user.getId()).thenReturn(userId);
    when(user.getDisplayName()).thenReturn("Legacy Owner");
    when(users.findByIdForPersonalWorkspaceReconciliation(userId))
        .thenReturn(Optional.of(user));

    assertThat(service.requirePersonalWorkspaceId(userId)).isEqualTo(userId);

    verify(workspaces).save(org.mockito.ArgumentMatchers.any(WorkspaceEntity.class));
    verify(memberships)
        .save(org.mockito.ArgumentMatchers.any(WorkspaceMembershipEntity.class));
  }

  /** Maps only the caller's supplied membership rows and exposes the caller-specific role. */
  @Test
  void listsOnlyCallerMembershipsWithCallerRole() {
    UUID ownerId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID();
    WorkspaceEntity workspace = WorkspaceEntity.personal(ownerId, "Workspace Owner", NOW);
    WorkspaceEntity personalWorkspace = WorkspaceEntity.personal(callerId, "Reviewer", NOW);
    WorkspaceMembershipEntity personalMembership =
        WorkspaceMembershipEntity.personalOwner(callerId, NOW);
    WorkspaceMembershipEntity membership =
        WorkspaceMembershipEntity.create(
            ownerId, callerId, WorkspaceRole.QA_REVIEWER, ownerId, NOW);
    UserEntity caller = mock(UserEntity.class);
    when(caller.getId()).thenReturn(callerId);
    when(users.findByIdForPersonalWorkspaceReconciliation(callerId))
        .thenReturn(Optional.of(caller));
    when(workspaces.findById(callerId)).thenReturn(Optional.of(personalWorkspace));
    when(memberships.findByWorkspaceIdAndUserId(callerId, callerId))
        .thenReturn(Optional.of(personalMembership));
    when(memberships.findAllByUserIdOrderByCreatedAtAsc(callerId))
        .thenReturn(List.of(membership));
    when(workspaces.findById(ownerId)).thenReturn(Optional.of(workspace));

    List<WorkspaceResponse> response = service.list(callerId);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).id()).isEqualTo(ownerId);
    assertThat(response.get(0).callerRole()).isEqualTo(WorkspaceRole.QA_REVIEWER);
  }

  /** Reconciles deterministic personal rows when an old binary created only the user. */
  @Test
  void reconcilesOldBinaryUserBeforeListingWorkspaces() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    when(user.getId()).thenReturn(userId);
    when(user.getDisplayName()).thenReturn("Old Binary User");
    when(users.findByIdForPersonalWorkspaceReconciliation(userId))
        .thenReturn(Optional.of(user));
    WorkspaceEntity workspace = WorkspaceEntity.personal(userId, "Old Binary User", NOW);
    WorkspaceMembershipEntity membership =
        WorkspaceMembershipEntity.personalOwner(userId, NOW);
    when(workspaces.findById(userId)).thenReturn(Optional.empty(), Optional.of(workspace));
    when(memberships.findByWorkspaceIdAndUserId(userId, userId))
        .thenReturn(Optional.empty());
    when(memberships.findAllByUserIdOrderByCreatedAtAsc(userId))
        .thenReturn(List.of(membership));

    List<WorkspaceResponse> response = service.list(userId);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).id()).isEqualTo(userId);
    assertThat(response.get(0).callerRole()).isEqualTo(WorkspaceRole.OWNER);
    verify(workspaces).save(org.mockito.ArgumentMatchers.any(WorkspaceEntity.class));
    verify(memberships)
        .save(org.mockito.ArgumentMatchers.any(WorkspaceMembershipEntity.class));
  }

  /** Fails closed when a deterministic workspace row has conflicting ownership metadata. */
  @Test
  void rejectsMalformedPersonalWorkspaceInvariant() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    WorkspaceEntity malformed = mock(WorkspaceEntity.class);
    when(user.getId()).thenReturn(userId);
    when(users.findByIdForPersonalWorkspaceReconciliation(userId))
        .thenReturn(Optional.of(user));
    when(malformed.getId()).thenReturn(userId);
    when(malformed.getCreatedBy()).thenReturn(UUID.randomUUID());
    when(workspaces.findById(userId)).thenReturn(Optional.of(malformed));

    assertThatThrownBy(() -> service.requirePersonalWorkspaceId(userId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Personal workspace invariant is violated.");

    verify(memberships, never()).save(org.mockito.ArgumentMatchers.any());
  }

  /** Fails closed when the deterministic membership carries a non-owner role. */
  @Test
  void rejectsMalformedPersonalMembershipInvariant() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    WorkspaceEntity workspace = WorkspaceEntity.personal(userId, "Quality Owner", NOW);
    WorkspaceMembershipEntity malformed = mock(WorkspaceMembershipEntity.class);
    when(user.getId()).thenReturn(userId);
    when(users.findByIdForPersonalWorkspaceReconciliation(userId))
        .thenReturn(Optional.of(user));
    when(workspaces.findById(userId)).thenReturn(Optional.of(workspace));
    when(malformed.getId()).thenReturn(userId);
    when(malformed.getWorkspaceId()).thenReturn(userId);
    when(malformed.getUserId()).thenReturn(userId);
    when(malformed.getCreatedBy()).thenReturn(userId);
    when(malformed.getRole()).thenReturn(WorkspaceRole.STAKEHOLDER);
    when(memberships.findByWorkspaceIdAndUserId(userId, userId))
        .thenReturn(Optional.of(malformed));

    assertThatThrownBy(() -> service.requirePersonalWorkspaceId(userId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Personal workspace invariant is violated.");
  }

  /** Fails closed instead of masking an orphaned deterministic membership. */
  @Test
  void rejectsPersonalMembershipWhoseWorkspaceIsMissing() {
    UUID userId = UUID.randomUUID();
    UserEntity user = mock(UserEntity.class);
    WorkspaceMembershipEntity membership =
        WorkspaceMembershipEntity.personalOwner(userId, NOW);
    when(user.getId()).thenReturn(userId);
    when(users.findByIdForPersonalWorkspaceReconciliation(userId))
        .thenReturn(Optional.of(user));
    when(workspaces.findById(userId)).thenReturn(Optional.empty());
    when(memberships.findByWorkspaceIdAndUserId(userId, userId))
        .thenReturn(Optional.of(membership));

    assertThatThrownBy(() -> service.requirePersonalWorkspaceId(userId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Personal workspace invariant is violated.");

    verify(workspaces, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
