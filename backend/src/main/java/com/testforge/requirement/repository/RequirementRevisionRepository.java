package com.testforge.requirement.repository;

import com.testforge.requirement.domain.RequirementRevisionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRevisionRepository
    extends JpaRepository<RequirementRevisionEntity, UUID> {
  /** Counts by requirement id matching the supplied criteria. */
  long countByRequirementId(UUID requirementId);
}
