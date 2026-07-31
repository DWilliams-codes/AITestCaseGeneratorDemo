package com.testforge.user.repository;

import com.testforge.user.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByEmailNormalized(String emailNormalized);

  boolean existsByEmailNormalized(String emailNormalized);
}
