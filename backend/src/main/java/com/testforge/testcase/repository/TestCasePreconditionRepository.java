package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCasePreconditionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCasePreconditionRepository
    extends JpaRepository<TestCasePreconditionEntity, UUID> {
  /** Batch-loads ordered preconditions for a bounded test-case page. */
  List<TestCasePreconditionEntity> findAllByTestCaseIdInOrderByTestCaseIdAscSortOrderAsc(
      List<UUID> testCaseIds);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
