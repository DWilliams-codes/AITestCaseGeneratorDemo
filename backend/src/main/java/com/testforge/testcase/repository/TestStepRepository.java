package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestStepRepository extends JpaRepository<TestStepEntity, UUID> {
  /** Finds all by test case id order by step number for the supplied criteria. */
  List<TestStepEntity> findAllByTestCaseIdOrderByStepNumber(UUID testCaseId);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
