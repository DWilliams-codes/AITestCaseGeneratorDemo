package com.testforge.generation.repository;

import com.testforge.generation.domain.GenerationCriterionSnapshotEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationCriterionSnapshotRepository
    extends JpaRepository<GenerationCriterionSnapshotEntity, Long> {
  /** Returns one generation set's immutable criterion evidence in source order. */
  List<GenerationCriterionSnapshotEntity> findAllByGenerationRunIdOrderBySortOrder(UUID runId);

  /** Resolves one snapshot by its stable source key within a generation set. */
  Optional<GenerationCriterionSnapshotEntity> findByGenerationRunIdAndCriterionKey(
      UUID runId, String criterionKey);

  /** Returns immutable criteria for several bounded generation sets. */
  List<GenerationCriterionSnapshotEntity> findAllByGenerationRunIdIn(List<UUID> runIds);
}
