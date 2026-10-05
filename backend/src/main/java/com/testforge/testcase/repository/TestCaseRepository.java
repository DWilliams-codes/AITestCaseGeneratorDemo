package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.domain.TestPriority;
import jakarta.persistence.LockModeType;
import java.util.List;
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

  /**
   * Queries one immutable generation set before pagination, using only parameterized literal
   * search.
   */
  @Query(
      "select distinct testCase from TestCaseEntity testCase "
          + "left join SnapshotTraceabilityLinkEntity link on link.testCaseId = testCase.id "
          + "left join GenerationCriterionSnapshotEntity snapshot on snapshot.id = link.criterionSnapshotId "
          + "where testCase.requirementId = :requirementId and testCase.generationRunId = :generationRunId "
          + "and (:status is null or testCase.status = :status) "
          + "and (:category is null or testCase.category = :category) "
          + "and (:priority is null or testCase.priority = :priority) "
          + "and (lower(testCase.testCaseKey) like lower(:searchPattern) escape '\\' "
          + "or lower(testCase.title) like lower(:searchPattern) escape '\\' "
          + "or lower(testCase.objective) like lower(:searchPattern) escape '\\' "
          + "or lower(snapshot.criterionKey) like lower(:searchPattern) escape '\\')")
  Page<TestCaseEntity> queryPage(
      UUID requirementId,
      UUID generationRunId,
      String searchPattern,
      TestCaseStatus status,
      TestCaseCategory category,
      TestPriority priority,
      Pageable pageable);

  /**
   * Queries one immutable generation set in the business priority order rather than enum text
   * order.
   */
  @Query(
      "select testCase from TestCaseEntity testCase "
          + "where testCase.requirementId = :requirementId and testCase.generationRunId = :generationRunId "
          + "and (:status is null or testCase.status = :status) "
          + "and (:category is null or testCase.category = :category) "
          + "and (:priority is null or testCase.priority = :priority) "
          + "and (lower(testCase.testCaseKey) like lower(:searchPattern) escape '\\' "
          + "or lower(testCase.title) like lower(:searchPattern) escape '\\' "
          + "or lower(testCase.objective) like lower(:searchPattern) escape '\\' "
          + "or exists (select link from SnapshotTraceabilityLinkEntity link, "
          + "GenerationCriterionSnapshotEntity snapshot where link.testCaseId = testCase.id "
          + "and snapshot.id = link.criterionSnapshotId and lower(snapshot.criterionKey) "
          + "like lower(:searchPattern) escape '\\')) "
          + "order by case "
          + "when testCase.priority = com.testforge.testcase.domain.TestPriority.CRITICAL then 0 "
          + "when testCase.priority = com.testforge.testcase.domain.TestPriority.HIGH then 1 "
          + "when testCase.priority = com.testforge.testcase.domain.TestPriority.MEDIUM then 2 "
          + "else 3 end asc, testCase.workItemNumber asc")
  Page<TestCaseEntity> queryPagePriorityDescending(
      UUID requirementId,
      UUID generationRunId,
      String searchPattern,
      TestCaseStatus status,
      TestCaseCategory category,
      TestPriority priority,
      Pageable pageable);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select testCase from TestCaseEntity testCase join RequirementEntity requirement on requirement.id = testCase.requirementId join ProjectEntity project on project.id = requirement.projectId where testCase.id = :testCaseId and project.ownerId = :ownerId")
  Optional<TestCaseEntity> findOwned(UUID testCaseId, UUID ownerId);

  /** Locks one case while a bridge revision receives its next sequence number. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select testCase from TestCaseEntity testCase where testCase.id = :testCaseId")
  Optional<TestCaseEntity> findByIdForUpdate(UUID testCaseId);

  /** Locks the bounded case graph roots before evidence and cascade deletion are rechecked. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select testCase from TestCaseEntity testCase where testCase.generationRunId = :generationRunId")
  List<TestCaseEntity> findAllByGenerationRunIdForUpdate(UUID generationRunId);
}
