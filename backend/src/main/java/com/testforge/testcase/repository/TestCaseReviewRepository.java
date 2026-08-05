package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseReviewEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestCaseReviewRepository extends JpaRepository<TestCaseReviewEntity, UUID> {
  /** Finds all by test case id order by created at desc for the supplied criteria. */
  List<TestCaseReviewEntity> findAllByTestCaseIdOrderByCreatedAtDesc(UUID testCaseId);

  /** Counts review evidence attached to cases produced by a generation set. */
  @Query(
      "select count(review) from TestCaseReviewEntity review join TestCaseEntity testCase on testCase.id = review.testCaseId where testCase.generationRunId = :runId")
  long countByGenerationRunId(UUID runId);
}
