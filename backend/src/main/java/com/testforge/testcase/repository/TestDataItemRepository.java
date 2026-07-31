package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestDataItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestDataItemRepository extends JpaRepository<TestDataItemEntity, UUID> {
  /** Finds all by test case id order by name for the supplied criteria. */
  List<TestDataItemEntity> findAllByTestCaseIdOrderByName(UUID testCaseId);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
