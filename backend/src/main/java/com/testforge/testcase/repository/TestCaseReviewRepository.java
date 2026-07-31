package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseReviewEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseReviewRepository extends JpaRepository<TestCaseReviewEntity, UUID> {
  List<TestCaseReviewEntity> findAllByTestCaseIdOrderByCreatedAtDesc(UUID testCaseId);
}
