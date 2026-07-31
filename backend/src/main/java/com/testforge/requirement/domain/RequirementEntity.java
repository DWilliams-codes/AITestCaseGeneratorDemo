package com.testforge.requirement.domain;

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
@Table(name = "requirements")
public class RequirementEntity {
  @Id private UUID id;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "user_story", nullable = false, length = 10000)
  private String userStory;

  @Column(name = "business_requirements", nullable = false, length = 20000)
  private String businessRequirements;

  @Column(nullable = false, length = 10000)
  private String assumptions;

  @Column(name = "source_reference", nullable = false, length = 1000)
  private String sourceReference;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private RequirementStatus status;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected RequirementEntity() {}

  private RequirementEntity(
      UUID id,
      UUID projectId,
      String title,
      String userStory,
      String businessRequirements,
      String assumptions,
      String sourceReference,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.projectId = projectId;
    this.title = title;
    this.userStory = userStory;
    this.businessRequirements = businessRequirements;
    this.assumptions = assumptions;
    this.sourceReference = sourceReference;
    this.status = RequirementStatus.DRAFT;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static RequirementEntity create(
      UUID projectId,
      String title,
      String userStory,
      String businessRequirements,
      String assumptions,
      String sourceReference,
      UUID createdBy,
      Instant now) {
    return new RequirementEntity(
        UUID.randomUUID(),
        projectId,
        title,
        userStory,
        businessRequirements,
        assumptions,
        sourceReference,
        createdBy,
        now);
  }

  public void update(
      String title,
      String userStory,
      String businessRequirements,
      String assumptions,
      String sourceReference,
      RequirementStatus status,
      Instant now) {
    this.title = title;
    this.userStory = userStory;
    this.businessRequirements = businessRequirements;
    this.assumptions = assumptions;
    this.sourceReference = sourceReference;
    this.status = status;
    this.updatedAt = now;
  }

  public void markGenerated(boolean needsClarification, Instant now) {
    this.status =
        needsClarification ? RequirementStatus.NEEDS_CLARIFICATION : RequirementStatus.GENERATED;
    this.updatedAt = now;
  }

  public void archive(Instant now) {
    this.status = RequirementStatus.ARCHIVED;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public String getTitle() {
    return title;
  }

  public String getUserStory() {
    return userStory;
  }

  public String getBusinessRequirements() {
    return businessRequirements;
  }

  public String getAssumptions() {
    return assumptions;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public RequirementStatus getStatus() {
    return status;
  }

  public UUID getCreatedBy() {
    return createdBy;
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
