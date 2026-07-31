package com.testforge.requirement.repository;

import com.testforge.requirement.domain.AcceptanceCriterionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AcceptanceCriterionRepository
    extends JpaRepository<AcceptanceCriterionEntity, UUID> {
  /** Finds all by requirement id order by sort order for the supplied criteria. */
  List<AcceptanceCriterionEntity> findAllByRequirementIdOrderBySortOrder(UUID requirementId);

  /** Counts by requirement id matching the supplied criteria. */
  long countByRequirementId(UUID requirementId);

  /** Deletes all by requirement id from persistent storage. */
  void deleteAllByRequirementId(UUID requirementId);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select criterion from AcceptanceCriterionEntity criterion join RequirementEntity requirement on requirement.id = criterion.requirementId join ProjectEntity project on project.id = requirement.projectId where criterion.id = :criterionId and project.ownerId = :ownerId")
  Optional<AcceptanceCriterionEntity> findOwned(UUID criterionId, UUID ownerId);
}
