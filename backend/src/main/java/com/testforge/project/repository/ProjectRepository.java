package com.testforge.project.repository;

import com.testforge.project.domain.ProjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
  Optional<ProjectEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

  Page<ProjectEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId, Pageable pageable);
}
