package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseRevisionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRevisionRepository extends JpaRepository<TestCaseRevisionEntity, UUID> {
  /** Counts by test case id matching the supplied criteria. */
  long countByTestCaseId(UUID testCaseId);
}
