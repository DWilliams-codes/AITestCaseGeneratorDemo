package com.testforge.traceability.domain;

import com.testforge.testcase.domain.CoverageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "traceability_links")
public class TraceabilityLinkEntity {
  @Id private UUID id;

  @Column(name = "acceptance_criterion_id", nullable = false)
  private UUID acceptanceCriterionId;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "coverage_type", nullable = false, length = 20)
  private CoverageType coverageType;

  @Column(nullable = false, precision = 5, scale = 4)
  private BigDecimal confidence;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Creates an empty TraceabilityLinkEntity instance for the persistence framework. */
  protected TraceabilityLinkEntity() {}

  /** Initializes TraceabilityLinkEntity with its required collaborators and domain state. */
  private TraceabilityLinkEntity(
      UUID id,
      UUID acceptanceCriterionId,
      UUID testCaseId,
      CoverageType coverageType,
      BigDecimal confidence,
      Instant createdAt) {
    this.id = id;
    this.acceptanceCriterionId = acceptanceCriterionId;
    this.testCaseId = testCaseId;
    this.coverageType = coverageType;
    this.confidence = confidence;
    this.createdAt = createdAt;
  }

  /** Creates a new TraceabilityLinkEntity initialized from the supplied domain values. */
  public static TraceabilityLinkEntity create(
      UUID acceptanceCriterionId,
      UUID testCaseId,
      CoverageType coverageType,
      BigDecimal confidence,
      Instant now) {
    return new TraceabilityLinkEntity(
        UUID.randomUUID(), acceptanceCriterionId, testCaseId, coverageType, confidence, now);
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current acceptance criterion id value. */
  public UUID getAcceptanceCriterionId() {
    return acceptanceCriterionId;
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
