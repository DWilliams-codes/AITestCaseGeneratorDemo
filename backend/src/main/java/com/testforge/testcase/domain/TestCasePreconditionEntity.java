package com.testforge.testcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "test_case_preconditions")
public class TestCasePreconditionEntity {
  @Id private UUID id;

  @Column(name = "test_case_id", nullable = false)
  private UUID testCaseId;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(nullable = false, length = 4000)
  private String description;

  protected TestCasePreconditionEntity() {}

  private TestCasePreconditionEntity(UUID id, UUID testCaseId, int sortOrder, String description) {
    this.id = id;
    this.testCaseId = testCaseId;
    this.sortOrder = sortOrder;
    this.description = description;
  }

  public static TestCasePreconditionEntity create(
      UUID testCaseId, int sortOrder, String description) {
    return new TestCasePreconditionEntity(UUID.randomUUID(), testCaseId, sortOrder, description);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTestCaseId() {
    return testCaseId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getDescription() {
    return description;
  }
}
