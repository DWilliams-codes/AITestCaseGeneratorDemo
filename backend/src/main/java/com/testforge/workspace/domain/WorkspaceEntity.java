package com.testforge.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class WorkspaceEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 160)
  private String name;

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

  /** Creates an empty workspace instance for the persistence framework. */
  protected WorkspaceEntity() {}

  /** Initializes an immutable personal-workspace identity and its audit fields. */
  private WorkspaceEntity(UUID id, String name, UUID createdBy, Instant now) {
    this.id = id;
    this.name = name;
    this.status = WorkspaceStatus.ACTIVE;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates the deterministic personal workspace owned by a registered user. */
  public static WorkspaceEntity personal(UUID userId, String displayName, Instant now) {
    return new WorkspaceEntity(userId, displayName.strip() + "'s Workspace", userId, now);
  }

  /** Returns the stable workspace identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the human-readable workspace name. */
  public String getName() {
    return name;
  }

  /** Returns the workspace lifecycle status. */
  public WorkspaceStatus getStatus() {
    return status;
  }

  /** Returns the user who created the workspace. */
  public UUID getCreatedBy() {
    return createdBy;
  }

  /** Returns the workspace creation timestamp. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the last workspace modification timestamp. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Returns the optimistic-lock version. */
  public long getVersion() {
    return version;
  }
}
