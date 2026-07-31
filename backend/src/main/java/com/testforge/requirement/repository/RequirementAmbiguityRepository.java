package com.testforge.requirement.repository;

import com.testforge.requirement.domain.RequirementAmbiguityEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequirementAmbiguityRepository
    extends JpaRepository<RequirementAmbiguityEntity, UUID> {
  /** Finds all by requirement id order by created at for the supplied criteria. */
  List<RequirementAmbiguityEntity> findAllByRequirementIdOrderByCreatedAt(UUID requirementId);

  /** Deletes all by requirement id and resolved false from persistent storage. */
  void deleteAllByRequirementIdAndResolvedFalse(UUID requirementId);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select ambiguity from RequirementAmbiguityEntity ambiguity join RequirementEntity requirement on requirement.id = ambiguity.requirementId join ProjectEntity project on project.id = requirement.projectId where ambiguity.id = :ambiguityId and project.ownerId = :ownerId")
  Optional<RequirementAmbiguityEntity> findOwned(UUID ambiguityId, UUID ownerId);
}
