package com.testforge.auth.repository;

import com.testforge.auth.domain.RefreshTokenEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  List<RefreshTokenEntity> findAllByFamilyId(UUID familyId);

  @Modifying
  @Query(
      "update RefreshTokenEntity token set token.revokedAt = :now where token.familyId = :familyId and token.revokedAt is null")
  int revokeFamily(UUID familyId, Instant now);
}
