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

  @Column(name = "work_item_number", nullable = false, updatable = false)
  private long workItemNumber;

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

  /** Creates an empty RequirementEntity instance for the persistence framework. */
  protected RequirementEntity() {}

  /** Initializes RequirementEntity with its required collaborators and domain state. */
  private RequirementEntity(
      UUID id,
      long workItemNumber,
      UUID projectId,
      String title,
      String userStory,
      String businessRequirements,
      String assumptions,
      String sourceReference,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.workItemNumber = workItemNumber;
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

  /** Creates a new RequirementEntity initialized from the supplied domain values. */
  public static RequirementEntity create(
      long workItemNumber,
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
        workItemNumber,
        projectId,
        title,
        userStory,
        businessRequirements,
        assumptions,
        sourceReference,
        createdBy,
        now);
  }

  /** Updates the entity's mutable domain state and modification timestamp. */
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

  /** Moves the requirement into its generated or clarification-needed state. */
  public void markGenerated(boolean needsClarification, Instant now) {
    this.status =
        needsClarification ? RequirementStatus.NEEDS_CLARIFICATION : RequirementStatus.GENERATED;
    this.updatedAt = now;
  }

  /** Marks the entity as archived and records its modification time. */
  public void archive(Instant now) {
    this.status = RequirementStatus.ARCHIVED;
    this.updatedAt = now;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current work item number value. */
  public long getWorkItemNumber() {
    return workItemNumber;
  }

  /** Returns the current project id value. */
  public UUID getProjectId() {
    return projectId;
  }

  /** Returns the current title value. */
  public String getTitle() {
    return title;
  }

  /** Returns the current user story value. */
  public String getUserStory() {
    return userStory;
  }

  /** Returns the current business requirements value. */
  public String getBusinessRequirements() {
    return businessRequirements;
  }

  /** Returns the current assumptions value. */
  public String getAssumptions() {
    return assumptions;
  }

  /** Returns the current source reference value. */
  public String getSourceReference() {
    return sourceReference;
  }

  /** Returns the current status value. */
  public RequirementStatus getStatus() {
    return status;
  }

  /** Returns the current created by value. */
  public UUID getCreatedBy() {
    return createdBy;
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
