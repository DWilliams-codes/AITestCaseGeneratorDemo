package com.testforge.user.repository;

import com.testforge.user.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  /** Finds by email normalized for the supplied criteria. */
  Optional<UserEntity> findByEmailNormalized(String emailNormalized);

  /** Determines whether by email normalized. */
  boolean existsByEmailNormalized(String emailNormalized);
}
