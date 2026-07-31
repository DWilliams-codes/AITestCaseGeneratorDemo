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

  protected UserEntity() {}

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

  public static UserEntity create(
      String email, String emailNormalized, String displayName, String passwordHash, Instant now) {
    return new UserEntity(
        UUID.randomUUID(), email, emailNormalized, displayName, passwordHash, now);
  }

  public void recordLogin(Instant now) {
    this.lastLoginAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }
}
