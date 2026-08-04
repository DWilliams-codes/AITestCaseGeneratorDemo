package com.testforge.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "workspace_memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
public class WorkspaceMembershipEntity {
  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private WorkspaceRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkspaceStatus status;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  /** Creates an empty workspace-membership instance for the persistence framework. */
  protected WorkspaceMembershipEntity() {}

  /** Initializes a membership with its role, lifecycle status, and audit fields. */
  private WorkspaceMembershipEntity(
      UUID id,
      UUID workspaceId,
      UUID userId,
      WorkspaceRole role,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.role = role;
    this.status = WorkspaceStatus.ACTIVE;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates the deterministic OWNER membership for a user's personal workspace. */
  public static WorkspaceMembershipEntity personalOwner(UUID userId, Instant now) {
    return new WorkspaceMembershipEntity(userId, userId, userId, WorkspaceRole.OWNER, userId, now);
  }

  /** Creates an additional active membership for a synthetic or future workflow. */
  public static WorkspaceMembershipEntity create(
      UUID workspaceId,
      UUID userId,
      WorkspaceRole role,
      UUID createdBy,
      Instant now) {
    return new WorkspaceMembershipEntity(
        UUID.randomUUID(), workspaceId, userId, role, createdBy, now);
  }

  /** Returns the stable membership identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the workspace referenced by this membership. */
  public UUID getWorkspaceId() {
    return workspaceId;
  }

  /** Returns the member user identifier. */
  public UUID getUserId() {
    return userId;
  }

  /** Returns the member's workspace role. */
  public WorkspaceRole getRole() {
    return role;
  }

  /** Returns the membership lifecycle status. */
  public WorkspaceStatus getStatus() {
    return status;
  }

  /** Returns the user who created the membership. */
  public UUID getCreatedBy() {
    return createdBy;
  }

  /** Returns the membership creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the last membership modification timestamp. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Returns the optimistic-lock version. */
  public long getVersion() {
    return version;
  }
}
