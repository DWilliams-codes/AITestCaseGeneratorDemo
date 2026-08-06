package com.testforge.auth.application;

import com.testforge.audit.application.AuditMetadata;
import com.testforge.audit.application.AuditService;
import com.testforge.auth.domain.RefreshTokenEntity;
import com.testforge.auth.repository.RefreshTokenRepository;
import com.testforge.config.AuthProperties;
import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits refresh rotation or replay containment before HTTP authentication outcomes are raised.
 */
@Service
public class RefreshTokenRotationService {
  private final RefreshTokenRepository tokens;
  private final UserRepository users;
  private final AuthProperties properties;
  private final Clock clock;
  private final AuditService audit;

  /** Initializes RefreshTokenRotationService with its required collaborators and domain state. */
  public RefreshTokenRotationService(
      RefreshTokenRepository tokens,
      UserRepository users,
      AuthProperties properties,
      Clock clock,
      AuditService audit) {
    this.tokens = tokens;
    this.users = users;
    this.properties = properties;
    this.clock = clock;
    this.audit = audit;
  }

  /** Locks and consumes one predecessor so concurrent rotations cannot fork a token family. */
  @Transactional
  public RotationOutcome rotate(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return RotationOutcome.rejected(RotationFailure.MISSING);
    }
    Instant now = clock.instant();
    RefreshTokenEntity current =
        tokens.findByTokenHashForUpdate(RefreshTokens.hash(rawToken)).orElse(null);
    if (current == null) {
      return RotationOutcome.rejected(RotationFailure.INVALID);
    }
    if (current.isRevoked()) {
      current.markReuseDetected(now);
      tokens.revokeFamily(current.getFamilyId(), now);
      audit.record(
          current.getUserId(),
          null,
          "USER",
          current.getUserId(),
          "TOKEN_REUSE_DETECTED",
          AuditMetadata.empty());
      return RotationOutcome.rejected(RotationFailure.REPLAY);
    }
    if (current.isExpired(now)) {
      current.revoke(now);
      return RotationOutcome.rejected(RotationFailure.EXPIRED);
    }
    UserEntity user =
        users.findById(current.getUserId()).filter(UserEntity::isEnabled).orElse(null);
    if (user == null) {
      current.revoke(now);
      tokens.revokeFamily(current.getFamilyId(), now);
      return RotationOutcome.rejected(RotationFailure.ACCOUNT_UNAVAILABLE);
    }

    String replacementBearer = RefreshTokens.generate();
    RefreshTokenEntity replacement =
        tokens.save(
            RefreshTokenEntity.create(
                user.getId(),
                current.getFamilyId(),
                RefreshTokens.hash(replacementBearer),
                now,
                now.plus(properties.refreshTokenTtl())));
    current.rotateTo(replacement.getId(), now);
    audit.record(
        user.getId(), null, "USER", user.getId(), "TOKEN_REFRESHED", AuditMetadata.empty());
    return RotationOutcome.success(user, replacement.getId(), replacementBearer);
  }

  public enum RotationFailure {
    MISSING,
    INVALID,
    EXPIRED,
    REPLAY,
    ACCOUNT_UNAVAILABLE
  }

  public record RotationOutcome(
      UserEntity user, UUID tokenId, String refreshToken, RotationFailure failure) {
    /** Executes the success operation for RotationOutcome. */
    static RotationOutcome success(UserEntity user, UUID tokenId, String refreshToken) {
      return new RotationOutcome(user, tokenId, refreshToken, null);
    }

    /** Executes the rejected operation for RotationOutcome. */
    static RotationOutcome rejected(RotationFailure failure) {
      return new RotationOutcome(null, null, null, failure);
    }

    /** Executes the succeeded operation for RotationOutcome. */
    public boolean succeeded() {
      return failure == null;
    }
  }
}
