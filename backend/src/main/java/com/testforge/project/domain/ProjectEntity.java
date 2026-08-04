package com.testforge.project.domain;

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
@Table(name = "projects")
public class ProjectEntity {
  @Id private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "workspace_id")
  private UUID workspaceId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  /** Creates an empty ProjectEntity instance for the persistence framework. */
  protected ProjectEntity() {}

  /** Initializes ProjectEntity with its required collaborators and domain state. */
  private ProjectEntity(
      UUID id,
      UUID ownerId,
      UUID workspaceId,
      String name,
      String description,
      ProjectStatus status,
      Instant now) {
    this.id = id;
    this.ownerId = ownerId;
    this.workspaceId = workspaceId;
    this.name = name;
    this.description = description;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a new ProjectEntity initialized from the supplied domain values. */
  public static ProjectEntity create(
      UUID ownerId, UUID workspaceId, String name, String description, Instant now) {
    return new ProjectEntity(
        UUID.randomUUID(), ownerId, workspaceId, name, description, ProjectStatus.ACTIVE, now);
  }

  /** Updates the entity's mutable domain state and modification timestamp. */
  public void update(String name, String description, Instant now) {
    this.name = name;
    this.description = description;
    this.updatedAt = now;
  }

  /** Marks the entity as archived and records its modification time. */
  public void archive(Instant now) {
    this.status = ProjectStatus.ARCHIVED;
    this.updatedAt = now;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current owner id value. */
  public UUID getOwnerId() {
    return ownerId;
  }

  /** Returns the workspace assigned during the tenancy compatibility bridge. */
  public UUID getWorkspaceId() {
    return workspaceId;
  }

  /** Returns the current name value. */
  public String getName() {
    return name;
  }

  /** Returns the current description value. */
  public String getDescription() {
    return description;
  }

  /** Returns the current status value. */
  public ProjectStatus getStatus() {
    return status;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the current updated at value. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Returns the current version value. */
  public long getVersion() {
    return version;
  }
}
