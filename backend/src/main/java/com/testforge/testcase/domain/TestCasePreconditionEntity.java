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

  /** Creates an empty TestCasePreconditionEntity instance for the persistence framework. */
  protected TestCasePreconditionEntity() {}

  /** Initializes TestCasePreconditionEntity with its required collaborators and domain state. */
  private TestCasePreconditionEntity(UUID id, UUID testCaseId, int sortOrder, String description) {
    this.id = id;
    this.testCaseId = testCaseId;
    this.sortOrder = sortOrder;
    this.description = description;
  }

  /** Creates a new TestCasePreconditionEntity initialized from the supplied domain values. */
  public static TestCasePreconditionEntity create(
      UUID testCaseId, int sortOrder, String description) {
    return new TestCasePreconditionEntity(UUID.randomUUID(), testCaseId, sortOrder, description);
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current test case id value. */
  public UUID getTestCaseId() {
    return testCaseId;
  }

  /** Returns the current sort order value. */
  public int getSortOrder() {
    return sortOrder;
  }

  /** Returns the current description value. */
  public String getDescription() {
    return description;
  }
}
