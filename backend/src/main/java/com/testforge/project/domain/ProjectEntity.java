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

  protected ProjectEntity() {}

  private ProjectEntity(
      UUID id, UUID ownerId, String name, String description, ProjectStatus status, Instant now) {
    this.id = id;
    this.ownerId = ownerId;
    this.name = name;
    this.description = description;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static ProjectEntity create(UUID ownerId, String name, String description, Instant now) {
    return new ProjectEntity(
        UUID.randomUUID(), ownerId, name, description, ProjectStatus.ACTIVE, now);
  }

  public void update(String name, String description, Instant now) {
    this.name = name;
    this.description = description;
    this.updatedAt = now;
  }

  public void archive(Instant now) {
    this.status = ProjectStatus.ARCHIVED;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
