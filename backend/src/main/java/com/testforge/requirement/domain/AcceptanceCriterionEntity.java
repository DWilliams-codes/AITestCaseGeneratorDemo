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

  protected AcceptanceCriterionEntity() {}

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

  public static AcceptanceCriterionEntity create(
      UUID requirementId, String criterionKey, String description, int sortOrder, Instant now) {
    return new AcceptanceCriterionEntity(
        UUID.randomUUID(), requirementId, criterionKey, description, sortOrder, now);
  }

  public void update(String criterionKey, String description, int sortOrder, Instant now) {
    this.criterionKey = criterionKey;
    this.description = description;
    this.sortOrder = sortOrder;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRequirementId() {
    return requirementId;
  }

  public String getCriterionKey() {
    return criterionKey;
  }

  public String getDescription() {
    return description;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
