package com.testforge.requirement.repository;

import com.testforge.requirement.domain.RequirementEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequirementRepository extends JpaRepository<RequirementEntity, UUID> {
  /** Finds all by project id order by updated at desc for the supplied criteria. */
  Page<RequirementEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId, Pageable pageable);

  /** Finds all by project id order by updated at desc for the supplied criteria. */
  List<RequirementEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId);

  /** Counts by project id matching the supplied criteria. */
  long countByProjectId(UUID projectId);

  /** Reports whether a project already contains the source story used by demo seeding. */
  boolean existsByProjectIdAndSourceReference(UUID projectId, String sourceReference);

  /** Finds owned for the supplied criteria. */
  @Query(
      "select requirement from RequirementEntity requirement join ProjectEntity project on project.id = requirement.projectId where requirement.id = :requirementId and project.ownerId = :ownerId")
  Optional<RequirementEntity> findOwned(UUID requirementId, UUID ownerId);
}
