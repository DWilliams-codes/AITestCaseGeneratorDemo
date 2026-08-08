package com.testforge.generation.repository;

import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, UUID> {
  /**
   * Finds by requirement id and requested by and idempotency key hash for the supplied criteria.
   */
  Optional<GenerationRunEntity> findByRequirementIdAndRequestedByAndIdempotencyKeyHash(
      UUID requirementId, UUID requestedBy, String idempotencyKeyHash);

  /** Finds all by requirement id order by started at desc for the supplied criteria. */
  Page<GenerationRunEntity> findAllByRequirementIdOrderByStartedAtDesc(
      UUID requirementId, Pageable pageable);

  /**
   * Finds first by requirement id and status order by completed at desc id desc for the supplied
   * criteria.
   */
  Optional<GenerationRunEntity> findFirstByRequirementIdAndStatusOrderByCompletedAtDescIdDesc(
      UUID requirementId, GenerationStatus status);

  /** Finds by id and requirement id and status for the supplied criteria. */
  Optional<GenerationRunEntity> findByIdAndRequirementIdAndStatus(
      UUID id, UUID requirementId, GenerationStatus status);

  /** Computes stable successful-set numbers for a bounded run page in one query. */
  @Query(
      value =
          """
          select cast(target.id as varchar) as runId,
                 case when target.status = 'COMPLETED' then count(prior.id) else 0 end as setNumber
          from testforge.generation_runs target
          left join testforge.generation_runs prior
            on prior.requirement_id = target.requirement_id
           and prior.status = 'COMPLETED'
           and (prior.completed_at < target.completed_at
                or (prior.completed_at = target.completed_at and prior.id <= target.id))
          where target.id in (:runIds)
          group by target.id, target.status
          """,
      nativeQuery = true)
  List<GenerationSetNumber> findSetNumbersByRunIds(@Param("runIds") List<UUID> runIds);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select run from GenerationRunEntity run join RequirementEntity requirement on requirement.id = run.requirementId join ProjectEntity project on project.id = requirement.projectId where run.id = :runId and project.ownerId = :ownerId")
  Optional<GenerationRunEntity> findOwned(UUID runId, UUID ownerId);

  /** Locks a pending run while one finalizer transitions it to a terminal state. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select run from GenerationRunEntity run where run.id = :runId")
  Optional<GenerationRunEntity> findByIdForUpdate(UUID runId);

  /** Locks a bounded generation-run set while legacy evidence is reconciled once. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select run from GenerationRunEntity run where run.id in :runIds")
  List<GenerationRunEntity> findAllByIdInForUpdate(List<UUID> runIds);

  interface GenerationSetNumber {
    /** Returns the generation-run identifier. */
    String getRunId();

    /** Returns the stable one-based successful-set number, or zero for a failed run. */
    long getSetNumber();
  }
}
