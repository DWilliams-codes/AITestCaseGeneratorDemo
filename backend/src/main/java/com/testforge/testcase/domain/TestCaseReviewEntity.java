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
@Table(name = "test_case_reviews")
public class TestCaseReviewEntity {
  @Id private UUID id;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Column(name = "reviewer_id", nullable = false)
  private UUID reviewerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReviewDecision decision;

  @Column(nullable = false, length = 4000)
  private String comments;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TestCaseReviewEntity() {}

  private TestCaseReviewEntity(
      UUID id,
      UUID testCaseId,
      UUID reviewerId,
      ReviewDecision decision,
      String comments,
      Instant now) {
    this.id = id;
    this.testCaseId = testCaseId;
    this.reviewerId = reviewerId;
    this.decision = decision;
    this.comments = comments;
    this.createdAt = now;
  }

  public static TestCaseReviewEntity create(
      UUID testCaseId, UUID reviewerId, ReviewDecision decision, String comments, Instant now) {
    return new TestCaseReviewEntity(
        UUID.randomUUID(), testCaseId, reviewerId, decision, comments, now);
  }

  public ReviewDecision getDecision() {
    return decision;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTestCaseId() {
    return testCaseId;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public String getComments() {
    return comments;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
