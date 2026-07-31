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

  /** Creates an empty TestCaseReviewEntity instance for the persistence framework. */
  protected TestCaseReviewEntity() {}

  /** Initializes TestCaseReviewEntity with its required collaborators and domain state. */
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

  /** Creates a new TestCaseReviewEntity initialized from the supplied domain values. */
  public static TestCaseReviewEntity create(
      UUID testCaseId, UUID reviewerId, ReviewDecision decision, String comments, Instant now) {
    return new TestCaseReviewEntity(
        UUID.randomUUID(), testCaseId, reviewerId, decision, comments, now);
  }

  /** Returns the current decision value. */
  public ReviewDecision getDecision() {
    return decision;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current test case id value. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the current reviewer id value. */
  public UUID getReviewerId() {
    return reviewerId;
  }

  /** Returns the current comments value. */
  public String getComments() {
    return comments;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
