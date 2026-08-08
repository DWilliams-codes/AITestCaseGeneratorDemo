package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestDataItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestDataItemRepository extends JpaRepository<TestDataItemEntity, UUID> {
  /** Batch-loads ordered test data for a bounded test-case page. */
  List<TestDataItemEntity> findAllByTestCaseIdInOrderByTestCaseIdAscNameAsc(List<UUID> testCaseIds);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
