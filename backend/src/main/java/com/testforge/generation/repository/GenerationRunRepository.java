package com.testforge.generation.repository;

import com.testforge.generation.domain.GenerationRunEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, UUID> {
  Optional<GenerationRunEntity> findByRequirementIdAndRequestedByAndIdempotencyKeyHash(
      UUID requirementId, UUID requestedBy, String idempotencyKeyHash);

  List<GenerationRunEntity> findAllByRequirementIdOrderByStartedAtDesc(UUID requirementId);

  @Query(
      "select run from GenerationRunEntity run join RequirementEntity requirement on requirement.id = run.requirementId join ProjectEntity project on project.id = requirement.projectId where run.id = :runId and project.ownerId = :ownerId")
  Optional<GenerationRunEntity> findOwned(UUID runId, UUID ownerId);
}
