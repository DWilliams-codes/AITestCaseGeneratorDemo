package com.testforge.traceability.repository;

import com.testforge.traceability.domain.TraceabilityLinkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TraceabilityLinkRepository extends JpaRepository<TraceabilityLinkEntity, UUID> {
  /** Returns bridge-era links for cases belonging to a bounded generation-run set. */
  @Query(
      "select link from TraceabilityLinkEntity link join TestCaseEntity testCase on testCase.id = link.testCaseId where testCase.generationRunId in :runIds")
  List<TraceabilityLinkEntity> findAllByGenerationRunIdIn(List<UUID> runIds);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
