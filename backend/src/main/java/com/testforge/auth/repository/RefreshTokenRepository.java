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
  /** Finds by token hash for the supplied criteria. */
  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  /** Finds all by family id for the supplied criteria. */
  List<RefreshTokenEntity> findAllByFamilyId(UUID familyId);

  /** Executes the revoke family operation for RefreshTokenRepository. */
  @Modifying
  @Query(
      "update RefreshTokenEntity token set token.revokedAt = :now where token.familyId = :familyId and token.revokedAt is null")
  int revokeFamily(UUID familyId, Instant now);
}
