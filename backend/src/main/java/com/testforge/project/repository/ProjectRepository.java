package com.testforge.project.repository;

import com.testforge.project.domain.ProjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
  /** Finds by id and owner id for the supplied criteria. */
  Optional<ProjectEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

  /** Finds all by owner id order by updated at desc for the supplied criteria. */
  Page<ProjectEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId, Pageable pageable);

  /** Finds the earliest matching project so demo seeding remains idempotent across restarts. */
  Optional<ProjectEntity> findFirstByOwnerIdAndNameOrderByCreatedAtAsc(UUID ownerId, String name);
}
