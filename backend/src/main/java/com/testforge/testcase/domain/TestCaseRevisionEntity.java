package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", length = 30)
  private TestCaseRevisionChangeType changeType;

  @Column(name = "change_reason", length = 4000)
  private String changeReason;

  @Column(name = "source_audit_event_id")
  private UUID sourceAuditEventId;

  /** Creates an empty TestCaseRevisionEntity instance for the persistence framework. */
  protected TestCaseRevisionEntity() {}

  /** Initializes TestCaseRevisionEntity with its required collaborators and domain state. */
  private TestCaseRevisionEntity(
      UUID testCaseId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt,
      TestCaseRevisionChangeType changeType,
      String changeReason,
      UUID sourceAuditEventId) {
    this.id = UUID.randomUUID();
    this.testCaseId = testCaseId;
    this.revisionNumber = revisionNumber;
    this.snapshotJson = snapshotJson;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
    this.changeType = changeType;
    this.changeReason = changeReason;
    this.sourceAuditEventId = sourceAuditEventId;
  }

  /** Creates a new TestCaseRevisionEntity initialized from the supplied domain values. */
  public static TestCaseRevisionEntity create(
      UUID testCaseId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt) {
    return new TestCaseRevisionEntity(
        testCaseId,
        revisionNumber,
        snapshotJson,
        changedBy,
        changedAt,
        TestCaseRevisionChangeType.EDIT,
        null,
        null);
  }

  /** Records a controlled lifecycle revision while retaining its full domain reason. */
  public static TestCaseRevisionEntity reopen(
      UUID testCaseId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt,
      String reason) {
    return new TestCaseRevisionEntity(
        testCaseId,
        revisionNumber,
        snapshotJson,
        changedBy,
        changedAt,
        TestCaseRevisionChangeType.REOPEN,
        reason,
        null);
  }

  /** Transfers a legacy audit reason into owner-isolated revision evidence exactly once. */
  public static TestCaseRevisionEntity legacyReopen(
      UUID testCaseId,
      long revisionNumber,
      String snapshotJson,
      UUID changedBy,
      Instant changedAt,
      String reason,
      UUID sourceAuditEventId) {
    return new TestCaseRevisionEntity(
        testCaseId,
        revisionNumber,
        snapshotJson,
        changedBy,
        changedAt,
        TestCaseRevisionChangeType.LEGACY_REOPEN,
        reason,
        sourceAuditEventId);
  }

  /** Returns the revision identifier. */
  public UUID getId() {
    return id;
  }

  /** Returns the revised test-case identifier. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the stable one-based revision number. */
  public long getRevisionNumber() {
    return revisionNumber;
  }

  /** Returns the persisted structured snapshot JSON. */
  public String getSnapshotJson() {
    return snapshotJson;
  }

  /** Returns the actor who created the revision. */
  public UUID getChangedBy() {
    return changedBy;
  }

  /** Returns the server timestamp for the revision. */
  public Instant getChangedAt() {
    return changedAt;
  }

  /** Returns the current change type value. */
  public TestCaseRevisionChangeType getChangeType() {
    return changeType;
  }

  /** Returns the current change reason value. */
  public String getChangeReason() {
    return changeReason;
  }

  /** Returns the legacy audit source used for idempotent bridge reconciliation. */
  public UUID getSourceAuditEventId() {
    return sourceAuditEventId;
  }
}
