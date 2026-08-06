package com.testforge.requirement.repository;

import com.testforge.requirement.domain.RequirementEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RequirementRepository extends JpaRepository<RequirementEntity, UUID> {
  /** Finds all by project id order by updated at desc for the supplied criteria. */
  Page<RequirementEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId, Pageable pageable);

  /** Counts by project id matching the supplied criteria. */
  long countByProjectId(UUID projectId);

  /** Returns requirement totals for a bounded page of projects in one aggregate query. */
  @Query(
      "select requirement.projectId as projectId, count(requirement) as requirementCount from RequirementEntity requirement where requirement.projectId in :projectIds group by requirement.projectId")
  List<ProjectRequirementCount> countByProjectIds(List<UUID> projectIds);

  /** Reports whether a project already contains the source story used by demo seeding. */
  boolean existsByProjectIdAndSourceReference(UUID projectId, String sourceReference);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select requirement from RequirementEntity requirement join ProjectEntity project on project.id = requirement.projectId where requirement.id = :requirementId and project.ownerId = :ownerId")
  Optional<RequirementEntity> findOwned(UUID requirementId, UUID ownerId);

  /** Serializes generation claims and criterion mutations on the source aggregate. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select requirement from RequirementEntity requirement join ProjectEntity project on project.id = requirement.projectId where requirement.id = :requirementId and project.ownerId = :ownerId")
  Optional<RequirementEntity> findOwnedForUpdate(UUID requirementId, UUID ownerId);

  interface ProjectRequirementCount {
    /** Returns the grouped project identifier. */
    UUID getProjectId();

    /** Returns the number of requirements in the project. */
    long getRequirementCount();
  }
}
