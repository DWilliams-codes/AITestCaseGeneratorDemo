package com.testforge.traceability.domain;

import com.testforge.testcase.domain.CoverageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A test-case mapping to immutable criterion evidence for one generation set. */
@Entity
@Table(name = "snapshot_traceability_links")
public class SnapshotTraceabilityLinkEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "criterion_snapshot_id", nullable = false)
  private Long criterionSnapshotId;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "coverage_type", nullable = false, length = 20)
  private CoverageType coverageType;

  @Column(nullable = false, precision = 5, scale = 4)
  private BigDecimal confidence;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Creates an empty link for the persistence framework. */
  protected SnapshotTraceabilityLinkEntity() {}

  /** Creates a dual-written link to immutable criterion evidence. */
  public static SnapshotTraceabilityLinkEntity create(
      Long criterionSnapshotId,
      UUID testCaseId,
      CoverageType coverageType,
      BigDecimal confidence,
      Instant now) {
    SnapshotTraceabilityLinkEntity link = new SnapshotTraceabilityLinkEntity();
    link.criterionSnapshotId = criterionSnapshotId;
    link.testCaseId = testCaseId;
    link.coverageType = coverageType;
    link.confidence = confidence;
    link.createdAt = now;
    return link;
  }

  /** Returns the current id value. */
  public Long getId() {
    return id;
  }

  /** Returns the current criterion snapshot id value. */
  public Long getCriterionSnapshotId() {
    return criterionSnapshotId;
  }

  /** Returns the current test case id value. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the current coverage type value. */
  public CoverageType getCoverageType() {
    return coverageType;
  }

  /** Returns the current confidence value. */
  public BigDecimal getConfidence() {
    return confidence;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
