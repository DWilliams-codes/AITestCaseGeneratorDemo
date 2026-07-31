package com.testforge.requirement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "requirement_revisions")
public class RequirementRevisionEntity {
  @Id private UUID id;

  @Column(name = "requirement_id", nullable = false)
  private UUID requirementId;

  @Column(name = "revision_number", nullable = false)
  private long revisionNumber;

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
  private String snapshotJson;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** Creates an empty RequirementRevisionEntity instance for the persistence framework. */
  protected RequirementRevisionEntity() {}

  /** Initializes RequirementRevisionEntity with its required collaborators and domain state. */
  private RequirementRevisionEntity(
      UUID requirementId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt) {
    this.id = UUID.randomUUID();
    this.requirementId = requirementId;
    this.revisionNumber = revisionNumber;
    this.snapshotJson = snapshotJson;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
  }

  /** Creates a new RequirementRevisionEntity initialized from the supplied domain values. */
  public static RequirementRevisionEntity create(
      UUID requirementId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt) {
    return new RequirementRevisionEntity(
        requirementId, revisionNumber, snapshotJson, changedBy, changedAt);
  }
}
