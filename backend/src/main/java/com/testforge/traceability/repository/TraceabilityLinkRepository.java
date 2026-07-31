package com.testforge.traceability.repository;

import com.testforge.traceability.domain.TraceabilityLinkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceabilityLinkRepository extends JpaRepository<TraceabilityLinkEntity, UUID> {
  List<TraceabilityLinkEntity> findAllByAcceptanceCriterionIdIn(List<UUID> criterionIds);

  List<TraceabilityLinkEntity> findAllByTestCaseId(UUID testCaseId);

  void deleteAllByTestCaseId(UUID testCaseId);
}
