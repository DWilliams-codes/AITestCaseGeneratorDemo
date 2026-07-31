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

  protected TraceabilityLinkEntity() {}

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

  public static TraceabilityLinkEntity create(
      UUID acceptanceCriterionId,
      UUID testCaseId,
      CoverageType coverageType,
      BigDecimal confidence,
      Instant now) {
    return new TraceabilityLinkEntity(
        UUID.randomUUID(), acceptanceCriterionId, testCaseId, coverageType, confidence, now);
  }

  public UUID getId() {
    return id;
  }

  public UUID getAcceptanceCriterionId() {
    return acceptanceCriterionId;
  }

  public UUID getTestCaseId() {
    return testCaseId;
  }

  public CoverageType getCoverageType() {
    return coverageType;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
