package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "test_data_items")
public class TestDataItemEntity {
  @Id private UUID id;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 2000)
  private String description;

  @Column(name = "example_value", nullable = false, length = 1000)
  private String exampleValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DataSensitivity sensitivity;

  @Column(name = "generation_strategy", nullable = false, length = 100)
  private String generationStrategy;

  /** Creates an empty TestDataItemEntity instance for the persistence framework. */
  protected TestDataItemEntity() {}

  /** Initializes TestDataItemEntity with its required collaborators and domain state. */
  private TestDataItemEntity(
      UUID id,
      UUID testCaseId,
      String name,
      String description,
      String exampleValue,
      DataSensitivity sensitivity,
      String generationStrategy) {
    this.id = id;
    this.testCaseId = testCaseId;
    this.name = name;
    this.description = description;
    this.exampleValue = exampleValue;
    this.sensitivity = sensitivity;
    this.generationStrategy = generationStrategy;
  }

  /** Creates a new TestDataItemEntity initialized from the supplied domain values. */
  public static TestDataItemEntity create(
      UUID testCaseId,
      String name,
      String description,
      String exampleValue,
      DataSensitivity sensitivity,
      String generationStrategy) {
    return new TestDataItemEntity(
        UUID.randomUUID(),
        testCaseId,
        name,
        description,
        exampleValue,
        sensitivity,
        generationStrategy);
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current test case id value. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the current name value. */
  public String getName() {
    return name;
  }

  /** Returns the current description value. */
  public String getDescription() {
    return description;
  }

  /** Returns the current example value value. */
  public String getExampleValue() {
    return exampleValue;
  }

  /** Returns the current sensitivity value. */
  public DataSensitivity getSensitivity() {
    return sensitivity;
  }

  /** Returns the current generation strategy value. */
  public String getGenerationStrategy() {
    return generationStrategy;
  }
}
