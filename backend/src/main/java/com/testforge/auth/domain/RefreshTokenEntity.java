package com.testforge.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Stores refresh-token lineage and digests; raw bearer tokens never cross into persistence. */
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

  /** Creates an empty RefreshTokenEntity instance for the persistence framework. */
  protected RefreshTokenEntity() {}

  /** Initializes RefreshTokenEntity with its required collaborators and domain state. */
  private RefreshTokenEntity(
      UUID id, UUID userId, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.familyId = familyId;
    this.tokenHash = tokenHash;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  /** Creates a new RefreshTokenEntity initialized from the supplied domain values. */
  public static RefreshTokenEntity create(
      UUID userId, UUID familyId, String tokenHash, Instant now, Instant expiresAt) {
    return new RefreshTokenEntity(UUID.randomUUID(), userId, familyId, tokenHash, now, expiresAt);
  }

  /** Revokes the predecessor while retaining its replacement link for replay investigation. */
  public void rotateTo(UUID replacementId, Instant now) {
    this.replacedByTokenId = replacementId;
    this.revokedAt = now;
  }

  /** Makes the token unusable without deleting its family history. */
  public void revoke(Instant now) {
    this.revokedAt = now;
  }

  /** Records replay evidence before the service revokes the entire token family. */
  public void markReuseDetected(Instant now) {
    this.reuseDetected = true;
    this.revokedAt = now;
  }

  /** Reports whether expired. */
  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  /** Reports whether revoked. */
  public boolean isRevoked() {
    return revokedAt != null;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current user id value. */
  public UUID getUserId() {
    return userId;
  }

  /** Returns the current family id value. */
  public UUID getFamilyId() {
    return familyId;
  }

  /** Returns the current token hash value. */
  public String getTokenHash() {
    return tokenHash;
  }

  /** Returns the current expires at value. */
  public Instant getExpiresAt() {
    return expiresAt;
  }

  /** Returns the current replaced by token id value. */
  public UUID getReplacedByTokenId() {
    return replacedByTokenId;
  }

  /** Reports whether reuse detected. */
  public boolean isReuseDetected() {
    return reuseDetected;
  }
}
