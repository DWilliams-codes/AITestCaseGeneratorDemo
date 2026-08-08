package com.testforge.testcase.repository;

import com.testforge.testcase.domain.TestCaseReviewEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseReviewRepository extends JpaRepository<TestCaseReviewEntity, UUID> {
  /** Finds all by test case id order by created at desc for the supplied criteria. */
  Page<TestCaseReviewEntity> findAllByTestCaseIdOrderByCreatedAtDesc(
      UUID testCaseId, Pageable pageable);

  /** Batch-loads a bounded recent-review compatibility slice for each case in a page. */
  @Query(
      value =
          """
          select ranked.id, ranked.test_case_id, ranked.reviewer_id, ranked.decision,
                 ranked.comments, ranked.created_at
          from (
            select review.*,
                   row_number() over (
                     partition by review.test_case_id
                     order by review.created_at desc, review.id desc
                   ) as review_rank
            from testforge.test_case_reviews review
            where review.test_case_id in (:testCaseIds)
          ) ranked
          where ranked.review_rank <= :perCaseLimit
          order by ranked.test_case_id, ranked.created_at desc, ranked.id desc
          """,
      nativeQuery = true)
  List<TestCaseReviewEntity> findRecentByTestCaseIds(
      @Param("testCaseIds") List<UUID> testCaseIds, @Param("perCaseLimit") int perCaseLimit);

  /** Counts review evidence attached to cases produced by a generation set. */
  @Query(
      "select count(review) from TestCaseReviewEntity review join TestCaseEntity testCase on testCase.id = review.testCaseId where testCase.generationRunId = :runId")
  long countByGenerationRunId(UUID runId);
}
