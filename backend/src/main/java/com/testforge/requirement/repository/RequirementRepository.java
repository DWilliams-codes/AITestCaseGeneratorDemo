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
  Page<RequirementEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId, Pageable pageable);

  List<RequirementEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId);

  long countByProjectId(UUID projectId);

  @Query(
      "select requirement from RequirementEntity requirement join ProjectEntity project on project.id = requirement.projectId where requirement.id = :requirementId and project.ownerId = :ownerId")
  Optional<RequirementEntity> findOwned(UUID requirementId, UUID ownerId);
}
