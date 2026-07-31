package com.testforge.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_sessions")
public class RefreshTokenEntity {
  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "family_id", nullable = false)
  private UUID familyId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by_token_id")
  private UUID replacedByTokenId;

  @Column(name = "reuse_detected", nullable = false)
  private boolean reuseDetected;

  protected RefreshTokenEntity() {}

  private RefreshTokenEntity(
      UUID id, UUID userId, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.familyId = familyId;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public static RefreshTokenEntity create(
      UUID userId, UUID familyId, String tokenHash, Instant now, Instant expiresAt) {
    return new RefreshTokenEntity(UUID.randomUUID(), userId, familyId, tokenHash, now, expiresAt);
  }

  public void rotateTo(UUID replacementId, Instant now) {
    this.replacedByTokenId = replacementId;
    this.revokedAt = now;
  }

  public void revoke(Instant now) {
    this.revokedAt = now;
  }

  public void markReuseDetected(Instant now) {
    this.reuseDetected = true;
    this.revokedAt = now;
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getFamilyId() {
    return familyId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public UUID getReplacedByTokenId() {
    return replacedByTokenId;
  }

  public boolean isReuseDetected() {
    return reuseDetected;
  }
}
