package com.testforge.requirement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "acceptance_criteria")
public class AcceptanceCriterionEntity {
  @Id private UUID id;

  @Column(name = "requirement_id", nullable = false)
  private UUID requirementId;

  @Column(name = "criterion_key", nullable = false, length = 20)
  private String criterionKey;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Creates an empty AcceptanceCriterionEntity instance for the persistence framework. */
  protected AcceptanceCriterionEntity() {}

  /** Initializes AcceptanceCriterionEntity with its required collaborators and domain state. */
  private AcceptanceCriterionEntity(
      UUID id,
      UUID requirementId,
      String criterionKey,
      String description,
      int sortOrder,
      Instant now) {
    this.id = id;
    this.requirementId = requirementId;
    this.criterionKey = criterionKey;
    this.description = description;
    this.sortOrder = sortOrder;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a new AcceptanceCriterionEntity initialized from the supplied domain values. */
  public static AcceptanceCriterionEntity create(
      UUID requirementId, String criterionKey, String description, int sortOrder, Instant now) {
    return new AcceptanceCriterionEntity(
        UUID.randomUUID(), requirementId, criterionKey, description, sortOrder, now);
  }

  /** Updates the entity's mutable domain state and modification timestamp. */
  public void update(String criterionKey, String description, int sortOrder, Instant now) {
    this.criterionKey = criterionKey;
    this.description = description;
    this.sortOrder = sortOrder;
    this.updatedAt = now;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current requirement id value. */
  public UUID getRequirementId() {
    return requirementId;
  }

  /** Returns the current criterion key value. */
  public String getCriterionKey() {
    return criterionKey;
  }

  /** Returns the current description value. */
  public String getDescription() {
    return description;
  }

  /** Returns the current sort order value. */
  public int getSortOrder() {
    return sortOrder;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the current updated at value. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
