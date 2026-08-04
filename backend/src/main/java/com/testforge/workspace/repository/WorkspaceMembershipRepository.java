package com.testforge.workspace.repository;

import com.testforge.workspace.domain.WorkspaceMembershipEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMembershipRepository
    extends JpaRepository<WorkspaceMembershipEntity, UUID> {
  /** Finds only memberships belonging to the supplied user in stable creation order. */
  List<WorkspaceMembershipEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

  /** Loads the exact membership used by the deterministic personal-workspace invariant. */
  Optional<WorkspaceMembershipEntity> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

  /** Counts exact deterministic rows for reconciliation and concurrency verification. */
  long countByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
