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

  protected TestDataItemEntity() {}

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

  public UUID getId() {
    return id;
  }

  public UUID getTestCaseId() {
    return testCaseId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getExampleValue() {
    return exampleValue;
  }

  public DataSensitivity getSensitivity() {
    return sensitivity;
  }

  public String getGenerationStrategy() {
    return generationStrategy;
  }
}
