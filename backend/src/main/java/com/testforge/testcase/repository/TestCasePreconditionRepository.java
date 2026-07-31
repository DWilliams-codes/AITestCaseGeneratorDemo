package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCasePreconditionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCasePreconditionRepository
    extends JpaRepository<TestCasePreconditionEntity, UUID> {
  List<TestCasePreconditionEntity> findAllByTestCaseIdOrderBySortOrder(UUID testCaseId);

  void deleteAllByTestCaseId(UUID testCaseId);
}
