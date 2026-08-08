package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCaseStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, UUID> {
  /**
   * Finds all by requirement id and generation run id order by work item number for the supplied
   * criteria.
   */
  Page<TestCaseEntity> findAllByRequirementIdAndGenerationRunIdOrderByWorkItemNumber(
      UUID requirementId, UUID generationRunId, Pageable pageable);

  /** Finds status-filtered cases in one immutable generation set. */
  Page<TestCaseEntity> findAllByRequirementIdAndGenerationRunIdAndStatusOrderByWorkItemNumber(
      UUID requirementId, UUID generationRunId, TestCaseStatus status, Pageable pageable);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select testCase from TestCaseEntity testCase join RequirementEntity requirement on requirement.id = testCase.requirementId join ProjectEntity project on project.id = requirement.projectId where testCase.id = :testCaseId and project.ownerId = :ownerId")
  Optional<TestCaseEntity> findOwned(UUID testCaseId, UUID ownerId);

  /** Locks one case while a bridge revision receives its next sequence number. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select testCase from TestCaseEntity testCase where testCase.id = :testCaseId")
  Optional<TestCaseEntity> findByIdForUpdate(UUID testCaseId);
}
