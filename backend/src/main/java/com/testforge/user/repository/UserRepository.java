package com.testforge.user.repository;

import com.testforge.user.domain.UserEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  /** Finds by email normalized for the supplied criteria. */
  Optional<UserEntity> findByEmailNormalized(String emailNormalized);

  /** Determines whether by email normalized. */
  boolean existsByEmailNormalized(String emailNormalized);

  /** Locks a user row while deterministic personal-workspace rows are reconciled. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserEntity u where u.id = :userId")
  Optional<UserEntity> findByIdForPersonalWorkspaceReconciliation(@Param("userId") UUID userId);
}
