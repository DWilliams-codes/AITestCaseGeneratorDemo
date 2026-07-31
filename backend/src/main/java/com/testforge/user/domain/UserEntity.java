package com.testforge.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(name = "email_normalized", nullable = false, length = 320, unique = true)
  private String emailNormalized;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  /** Creates an empty UserEntity instance for the persistence framework. */
  protected UserEntity() {}

  /** Initializes UserEntity with its required collaborators and domain state. */
  private UserEntity(
      UUID id,
      String email,
      String emailNormalized,
      String displayName,
      String passwordHash,
      Instant now) {
    this.id = id;
    this.email = email;
    this.emailNormalized = emailNormalized;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
    this.role = UserRole.USER;
    this.enabled = true;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Creates a new UserEntity initialized from the supplied domain values. */
  public static UserEntity create(
      String email, String emailNormalized, String displayName, String passwordHash, Instant now) {
    return new UserEntity(
        UUID.randomUUID(), email, emailNormalized, displayName, passwordHash, now);
  }

  /** Records login for the current operation. */
  public void recordLogin(Instant now) {
    this.lastLoginAt = now;
    this.updatedAt = now;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current email value. */
  public String getEmail() {
    return email;
  }

  /** Returns the current email normalized value. */
  public String getEmailNormalized() {
    return emailNormalized;
  }

  /** Returns the current display name value. */
  public String getDisplayName() {
    return displayName;
  }

  /** Returns the current password hash value. */
  public String getPasswordHash() {
    return passwordHash;
  }

  /** Returns the current role value. */
  public UserRole getRole() {
    return role;
  }

  /** Reports whether enabled. */
  public boolean isEnabled() {
    return enabled;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the current updated at value. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /** Returns the current last login at value. */
  public Instant getLastLoginAt() {
    return lastLoginAt;
  }
}
