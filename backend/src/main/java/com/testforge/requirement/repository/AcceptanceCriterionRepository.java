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

  /** Returns current criterion evidence for a bounded group of bridge-era requirements. */
  List<AcceptanceCriterionEntity> findAllByRequirementIdInOrderByRequirementIdAscSortOrderAsc(
      List<UUID> requirementIds);

  /** Returns criterion totals for a bounded page of requirements in one aggregate query. */
  @Query(
      "select criterion.requirementId as requirementId, count(criterion) as criterionCount from AcceptanceCriterionEntity criterion where criterion.requirementId in :requirementIds group by criterion.requirementId")
  List<RequirementCriterionCount> countByRequirementIds(List<UUID> requirementIds);

  /** Deletes all by requirement id from persistent storage. */
  void deleteAllByRequirementId(UUID requirementId);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select criterion from AcceptanceCriterionEntity criterion join RequirementEntity requirement on requirement.id = criterion.requirementId join ProjectEntity project on project.id = requirement.projectId where criterion.id = :criterionId and project.ownerId = :ownerId")
  Optional<AcceptanceCriterionEntity> findOwned(UUID criterionId, UUID ownerId);

  /** Resolves only the aggregate identifier during authorization, before locking that aggregate. */
  @Query(
      "select criterion.requirementId from AcceptanceCriterionEntity criterion join RequirementEntity requirement on requirement.id = criterion.requirementId join ProjectEntity project on project.id = requirement.projectId where criterion.id = :criterionId and project.ownerId = :ownerId")
  Optional<UUID> findOwnedRequirementId(UUID criterionId, UUID ownerId);

  interface RequirementCriterionCount {
    /** Returns the grouped requirement identifier. */
    UUID getRequirementId();

    /** Returns the number of acceptance criteria in the requirement. */
    long getCriterionCount();
  }
}
