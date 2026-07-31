package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "test_steps")
public class TestStepEntity {
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

  /** Creates an empty TestStepEntity instance for the persistence framework. */
  protected TestStepEntity() {}

  /** Initializes TestStepEntity with its required collaborators and domain state. */
  private TestStepEntity(
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

  /** Creates a new TestStepEntity initialized from the supplied domain values. */
  public static TestStepEntity create(
      UUID testCaseId,
      int stepNumber,
      String action,
      String expectedResult,
      String testDataReference) {
    return new TestStepEntity(
        UUID.randomUUID(), testCaseId, stepNumber, action, expectedResult, testDataReference);
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current test case id value. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the current step number value. */
  public int getStepNumber() {
    return stepNumber;
  }

  /** Returns the current action value. */
  public String getAction() {
    return action;
  }

  /** Returns the current expected result value. */
  public String getExpectedResult() {
    return expectedResult;
  }

  /** Returns the current test data reference value. */
  public String getTestDataReference() {
    return testDataReference;
  }
}
