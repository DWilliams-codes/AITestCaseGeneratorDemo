package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseRevisionEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestCaseRevisionRepository extends JpaRepository<TestCaseRevisionEntity, UUID> {
  /** Counts by test case id matching the supplied criteria. */
  long countByTestCaseId(UUID testCaseId);

  /** Reports whether one legacy audit event has already been transferred. */
  boolean existsBySourceAuditEventId(UUID sourceAuditEventId);

  /** Counts revision evidence attached to cases produced by a generation set. */
  @Query(
      "select count(revision) from TestCaseRevisionEntity revision join TestCaseEntity testCase on testCase.id = revision.testCaseId where testCase.generationRunId = :runId")
  long countByGenerationRunId(UUID runId);

  /** Returns a stable newest-first page for an already owner-checked case. */
  Page<TestCaseRevisionEntity> findAllByTestCaseIdOrderByChangedAtDescIdDesc(
      UUID testCaseId, Pageable pageable);
}
