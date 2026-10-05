package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseSetupStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseSetupStepRepository extends JpaRepository<TestCaseSetupStepEntity, UUID> {
  /** Batch-loads setup steps in response order for a bounded case set. */
  List<TestCaseSetupStepEntity> findAllByTestCaseIdInOrderByTestCaseIdAscStepNumberAsc(
      List<UUID> testCaseIds);

  /** Deletes replacement setup rows for one mutable test case. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
