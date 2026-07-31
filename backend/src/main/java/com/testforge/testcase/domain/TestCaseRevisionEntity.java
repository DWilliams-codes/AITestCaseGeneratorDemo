package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_case_revisions")
public class TestCaseRevisionEntity {
  @Id private UUID id;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Column(name = "revision_number", nullable = false)
  private long revisionNumber;

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
  private String snapshotJson;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  /** Creates an empty TestCaseRevisionEntity instance for the persistence framework. */
  protected TestCaseRevisionEntity() {}

  /** Initializes TestCaseRevisionEntity with its required collaborators and domain state. */
  private TestCaseRevisionEntity(
      UUID testCaseId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt) {
    this.id = UUID.randomUUID();
    this.testCaseId = testCaseId;
    this.revisionNumber = revisionNumber;
    this.snapshotJson = snapshotJson;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
  }

  /** Creates a new TestCaseRevisionEntity initialized from the supplied domain values. */
  public static TestCaseRevisionEntity create(
      UUID testCaseId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt) {
    return new TestCaseRevisionEntity(
        testCaseId, revisionNumber, snapshotJson, changedBy, changedAt);
  }
}
