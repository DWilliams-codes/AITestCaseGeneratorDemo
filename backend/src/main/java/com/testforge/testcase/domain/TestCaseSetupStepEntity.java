package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Stores reproducible environment setup separately from the evidence-bearing test procedure. */
@Entity
@Table(name = "test_case_setup_steps")
public class TestCaseSetupStepEntity {
  @Id private UUID id;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Column(name = "step_number", nullable = false)
  private int stepNumber;

  @Column(nullable = false, length = 4000)
  private String action;

  @Column(name = "expected_result", nullable = false, length = 4000)
  private String expectedResult;

  @Column(name = "test_data_reference", length = 1000)
  private String testDataReference;

  /** Creates an empty setup-step entity for the persistence framework. */
  protected TestCaseSetupStepEntity() {}

  /** Initializes one immutable setup-step row. */
  private TestCaseSetupStepEntity(
      UUID id,
      UUID testCaseId,
      int stepNumber,
      String action,
      String expectedResult,
      String testDataReference) {
    this.id = id;
    this.testCaseId = testCaseId;
    this.stepNumber = stepNumber;
    this.action = action;
    this.expectedResult = expectedResult;
    this.testDataReference = testDataReference;
  }

  /** Creates a persisted setup-step row for one test case. */
  public static TestCaseSetupStepEntity create(
      UUID testCaseId,
      int stepNumber,
      String action,
      String expectedResult,
      String testDataReference) {
    return new TestCaseSetupStepEntity(
        UUID.randomUUID(), testCaseId, stepNumber, action, expectedResult, testDataReference);
  }

  /** Returns the owning test case identifier. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the setup sequence number. */
  public int getStepNumber() {
    return stepNumber;
  }

  /** Returns the setup action. */
  public String getAction() {
    return action;
  }

  /** Returns the observable readiness result. */
  public String getExpectedResult() {
    return expectedResult;
  }

  /** Returns the optional test-data reference. */
  public String getTestDataReference() {
    return testDataReference;
  }
}
