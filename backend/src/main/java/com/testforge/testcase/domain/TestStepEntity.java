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

  protected TestStepEntity() {}

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

  public static TestStepEntity create(
      UUID testCaseId,
      int stepNumber,
      String action,
      String expectedResult,
      String testDataReference) {
    return new TestStepEntity(
        UUID.randomUUID(), testCaseId, stepNumber, action, expectedResult, testDataReference);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTestCaseId() {
    return testCaseId;
  }

  public int getStepNumber() {
    return stepNumber;
  }

  public String getAction() {
    return action;
  }

  public String getExpectedResult() {
    return expectedResult;
  }

  public String getTestDataReference() {
    return testDataReference;
  }
}
