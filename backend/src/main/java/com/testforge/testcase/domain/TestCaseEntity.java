package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_cases")
public class TestCaseEntity {
  @Id private UUID id;

  @Column(name = "requirement_id", nullable = false)
  private UUID requirementId;

  @Column(name = "generation_run_id", nullable = false)
  private UUID generationRunId;

  @Column(name = "test_case_key", nullable = false, length = 30)
  private String testCaseKey;

  @Column(nullable = false, length = 300)
  private String title;

  @Column(nullable = false, length = 4000)
  private String objective;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TestCaseCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level", nullable = false, length = 20)
  private TestPriority riskLevel;

  @Column(name = "automation_candidate", nullable = false)
  private boolean automationCandidate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TestCaseStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "coverage_intent", nullable = false, length = 40)
  private CoverageIntent coverageIntent;

  @Column(nullable = false, length = 4000)
  private String rationale;

  @Column(name = "final_expected_outcome", nullable = false, length = 4000)
  private String finalExpectedOutcome;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected TestCaseEntity() {}

  private TestCaseEntity(
      UUID id,
      UUID requirementId,
      UUID generationRunId,
      String testCaseKey,
      String title,
      String objective,
      TestCaseCategory category,
      TestPriority priority,
      TestPriority riskLevel,
      boolean automationCandidate,
      CoverageIntent coverageIntent,
      String rationale,
      String finalExpectedOutcome,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.requirementId = requirementId;
    this.generationRunId = generationRunId;
    this.testCaseKey = testCaseKey;
    this.title = title;
    this.objective = objective;
    this.category = category;
    this.priority = priority;
    this.riskLevel = riskLevel;
    this.automationCandidate = automationCandidate;
    this.status = TestCaseStatus.GENERATED;
    this.coverageIntent = coverageIntent;
    this.rationale = rationale;
    this.finalExpectedOutcome = finalExpectedOutcome;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static TestCaseEntity create(
      UUID requirementId,
      UUID generationRunId,
      String testCaseKey,
      String title,
      String objective,
      TestCaseCategory category,
      TestPriority priority,
      TestPriority riskLevel,
      boolean automationCandidate,
      CoverageIntent coverageIntent,
      String rationale,
      String finalExpectedOutcome,
      UUID createdBy,
      Instant now) {
    return new TestCaseEntity(
        UUID.randomUUID(),
        requirementId,
        generationRunId,
        testCaseKey,
        title,
        objective,
        category,
        priority,
        riskLevel,
        automationCandidate,
        coverageIntent,
        rationale,
        finalExpectedOutcome,
        createdBy,
        now);
  }

  public void update(
      String title,
      String objective,
      TestCaseCategory category,
      TestPriority priority,
      TestPriority riskLevel,
      boolean automationCandidate,
      String rationale,
      String finalExpectedOutcome,
      Instant now) {
    this.title = title;
    this.objective = objective;
    this.category = category;
    this.priority = priority;
    this.riskLevel = riskLevel;
    this.automationCandidate = automationCandidate;
    this.rationale = rationale;
    this.finalExpectedOutcome = finalExpectedOutcome;
    this.status = TestCaseStatus.IN_REVIEW;
    this.updatedAt = now;
  }

  public void review(ReviewDecision decision, Instant now) {
    this.status =
        switch (decision) {
          case APPROVED -> TestCaseStatus.APPROVED;
          case REJECTED -> TestCaseStatus.REJECTED;
          case CHANGES_REQUESTED -> TestCaseStatus.NEEDS_REVISION;
        };
    this.updatedAt = now;
  }

  public void reopen(Instant now) {
    this.status = TestCaseStatus.IN_REVIEW;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRequirementId() {
    return requirementId;
  }

  public UUID getGenerationRunId() {
    return generationRunId;
  }

  public String getTestCaseKey() {
    return testCaseKey;
  }

  public String getTitle() {
    return title;
  }

  public String getObjective() {
    return objective;
  }

  public TestCaseCategory getCategory() {
    return category;
  }

  public TestPriority getPriority() {
    return priority;
  }

  public TestPriority getRiskLevel() {
    return riskLevel;
  }

  public boolean isAutomationCandidate() {
    return automationCandidate;
  }

  public TestCaseStatus getStatus() {
    return status;
  }

  public CoverageIntent getCoverageIntent() {
    return coverageIntent;
  }

  public String getRationale() {
    return rationale;
  }

  public String getFinalExpectedOutcome() {
    return finalExpectedOutcome;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
