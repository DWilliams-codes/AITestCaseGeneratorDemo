package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestDataItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestDataItemRepository extends JpaRepository<TestDataItemEntity, UUID> {
  List<TestDataItemEntity> findAllByTestCaseIdOrderByName(UUID testCaseId);

  void deleteAllByTestCaseId(UUID testCaseId);
}
