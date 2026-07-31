package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestStepRepository extends JpaRepository<TestStepEntity, UUID> {
  List<TestStepEntity> findAllByTestCaseIdOrderByStepNumber(UUID testCaseId);

  void deleteAllByTestCaseId(UUID testCaseId);
}
