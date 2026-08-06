package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestStepRepository extends JpaRepository<TestStepEntity, UUID> {
  /** Batch-loads ordered steps for a bounded test-case page. */
  List<TestStepEntity> findAllByTestCaseIdInOrderByTestCaseIdAscStepNumberAsc(
      List<UUID> testCaseIds);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
