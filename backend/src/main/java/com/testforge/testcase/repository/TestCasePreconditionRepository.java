package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCasePreconditionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCasePreconditionRepository
    extends JpaRepository<TestCasePreconditionEntity, UUID> {
  /** Finds all by test case id order by sort order for the supplied criteria. */
  List<TestCasePreconditionEntity> findAllByTestCaseIdOrderBySortOrder(UUID testCaseId);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
