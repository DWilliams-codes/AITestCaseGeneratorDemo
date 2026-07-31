package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCaseStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, UUID> {
  /** Finds all by requirement id order by work item number for the supplied criteria. */
  List<TestCaseEntity> findAllByRequirementIdOrderByWorkItemNumber(UUID requirementId);

  /** Finds all by requirement id and status order by work item number for the supplied criteria. */
  List<TestCaseEntity> findAllByRequirementIdAndStatusOrderByWorkItemNumber(
      UUID requirementId, TestCaseStatus status);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select testCase from TestCaseEntity testCase join RequirementEntity requirement on requirement.id = testCase.requirementId join ProjectEntity project on project.id = requirement.projectId where testCase.id = :testCaseId and project.ownerId = :ownerId")
  Optional<TestCaseEntity> findOwned(UUID testCaseId, UUID ownerId);
}
