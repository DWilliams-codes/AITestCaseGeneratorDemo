package com.testforge.traceability.repository;

import com.testforge.traceability.domain.TraceabilityLinkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceabilityLinkRepository extends JpaRepository<TraceabilityLinkEntity, UUID> {
  /** Finds all by acceptance criterion id in for the supplied criteria. */
  List<TraceabilityLinkEntity> findAllByAcceptanceCriterionIdIn(List<UUID> criterionIds);

  /** Finds all by test case id for the supplied criteria. */
  List<TraceabilityLinkEntity> findAllByTestCaseId(UUID testCaseId);

  /** Deletes all by test case id from persistent storage. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
