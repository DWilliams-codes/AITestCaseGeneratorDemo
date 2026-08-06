package com.testforge.traceability.repository;

import com.testforge.traceability.domain.SnapshotTraceabilityLinkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotTraceabilityLinkRepository
    extends JpaRepository<SnapshotTraceabilityLinkEntity, Long> {
  /** Returns all links for a bounded immutable criterion set. */
  List<SnapshotTraceabilityLinkEntity> findAllByCriterionSnapshotIdIn(List<Long> snapshotIds);

  /** Returns immutable-evidence links for several bounded cases. */
  List<SnapshotTraceabilityLinkEntity> findAllByTestCaseIdIn(List<UUID> testCaseIds);

  /** Deletes current links before an active case remaps its immutable evidence. */
  void deleteAllByTestCaseId(UUID testCaseId);
}
