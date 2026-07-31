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

  @Column(name = "work_item_number", nullable = false, updatable = false)
  private long workItemNumber;

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

  /** Creates an empty TestCaseEntity instance for the persistence framework. */
  protected TestCaseEntity() {}

  /** Initializes TestCaseEntity with its required collaborators and domain state. */
  private TestCaseEntity(
      UUID id,
      long workItemNumber,
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
    this.workItemNumber = workItemNumber;
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

  /** Creates a new TestCaseEntity initialized from the supplied domain values. */
  public static TestCaseEntity create(
      long workItemNumber,
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
        workItemNumber,
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

  /** Updates the entity's mutable domain state and modification timestamp. */
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

  /** Executes the review operation for TestCaseEntity. */
  public void review(ReviewDecision decision, Instant now) {
    this.status =
        switch (decision) {
          case APPROVED -> TestCaseStatus.APPROVED;
          case REJECTED -> TestCaseStatus.REJECTED;
          case CHANGES_REQUESTED -> TestCaseStatus.NEEDS_REVISION;
        };
    this.updatedAt = now;
  }

  /** Executes the reopen operation for TestCaseEntity. */
  public void reopen(Instant now) {
    this.status = TestCaseStatus.IN_REVIEW;
    this.updatedAt = now;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current work item number value. */
  public long getWorkItemNumber() {
    return workItemNumber;
  }

  /** Returns the current requirement id value. */
  public UUID getRequirementId() {
    return requirementId;
  }

  /** Returns the current generation run id value. */
  public UUID getGenerationRunId() {
    return generationRunId;
  }

  /** Returns the current test case key value. */
  public String getTestCaseKey() {
    return testCaseKey;
  }

  /** Returns the current title value. */
  public String getTitle() {
    return title;
  }

  /** Returns the current objective value. */
  public String getObjective() {
    return objective;
  }

  /** Returns the current category value. */
  public TestCaseCategory getCategory() {
    return category;
  }

  /** Returns the current priority value. */
  public TestPriority getPriority() {
    return priority;
  }

  /** Returns the current risk level value. */
  public TestPriority getRiskLevel() {
    return riskLevel;
  }

  /** Reports whether automation candidate. */
  public boolean isAutomationCandidate() {
    return automationCandidate;
  }

  /** Returns the current status value. */
  public TestCaseStatus getStatus() {
    return status;
  }

  /** Returns the current coverage intent value. */
  public CoverageIntent getCoverageIntent() {
    return coverageIntent;
  }

  /** Returns the current rationale value. */
  public String getRationale() {
    return rationale;
  }

  /** Returns the current final expected outcome value. */
  public String getFinalExpectedOutcome() {
    return finalExpectedOutcome;
  }

  /** Returns the current created by value. */
  public UUID getCreatedBy() {
    return createdBy;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the current updated at value. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Returns the current version value. */
  public long getVersion() {
    return version;
  }
}
