package com.testforge.auth.application;

import com.testforge.audit.application.AuditService;
import com.testforge.auth.domain.RefreshTokenEntity;
import com.testforge.auth.dto.AuthDtos.LoginRequest;
import com.testforge.auth.dto.AuthDtos.RegisterRequest;
import com.testforge.auth.dto.AuthDtos.TokenResponse;
import com.testforge.auth.dto.AuthDtos.UserResponse;
import com.testforge.auth.repository.RefreshTokenRepository;
import com.testforge.common.error.ApiExceptions;
import com.testforge.config.AuthProperties;
import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import com.testforge.workspace.application.WorkspaceService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthProperties properties;
  private final Clock clock;
  private final AuditService auditService;
  private final WorkspaceService workspaceService;

  /** Initializes AuthService with its required collaborators and domain state. */
  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthProperties properties,
      Clock clock,
      AuditService auditService,
      WorkspaceService workspaceService) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.properties = properties;
    this.clock = clock;
    this.auditService = auditService;
    this.workspaceService = workspaceService;
  }

  /** Registers a new user and creates an authenticated session. */
  @Transactional
  public Session register(RegisterRequest request) {
    String email = request.email().strip();
    String normalized = normalizeEmail(email);
    if (users.existsByEmailNormalized(normalized)) {
      throw ApiExceptions.conflict("email_in_use", "An account already exists for this email.");
    }
    Instant now = clock.instant();
    UserEntity user =
        users.save(
            UserEntity.create(
                email,
                normalized,
                request.displayName().strip(),
                passwordEncoder.encode(request.password()),
                now));
    workspaceService.provisionPersonalWorkspace(user, now);
    auditService.record(user.getId(), null, "USER", user.getId(), "REGISTERED", Map.of());
    return newSession(user, UUID.randomUUID(), now);
  }

  /** Authenticates supplied credentials and creates a rotating session. */
  @Transactional
  public Session login(LoginRequest request) {
    UserEntity user =
        users
            .findByEmailNormalized(normalizeEmail(request.email()))
            .orElseThrow(() -> ApiExceptions.unauthorized("Email or password is incorrect."));
    if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw ApiExceptions.unauthorized("Email or password is incorrect.");
    }
    workspaceService.requirePersonalWorkspaceId(user.getId());
    Instant now = clock.instant();
    user.recordLogin(now);
    auditService.record(user.getId(), null, "USER", user.getId(), "LOGGED_IN", Map.of());
    return newSession(user, UUID.randomUUID(), now);
  }

  /** Creates the replacement before revoking its predecessor in the same transaction. */
  @Transactional
  public Session refresh(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw ApiExceptions.unauthorized("A refresh token is required.");
    }
    Instant now = clock.instant();
    RefreshTokenEntity current =
        refreshTokens
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> ApiExceptions.unauthorized("The refresh token is invalid."));
    if (current.isRevoked()) {
      current.markReuseDetected(now);
      refreshTokens.revokeFamily(current.getFamilyId(), now);
      throw ApiExceptions.unauthorized("Refresh token reuse was detected. Sign in again.");
    }
    if (current.isExpired(now)) {
      current.revoke(now);
      throw ApiExceptions.unauthorized("The refresh token has expired.");
    }
    UserEntity user =
        users
            .findById(current.getUserId())
            .filter(UserEntity::isEnabled)
            .orElseThrow(() -> ApiExceptions.unauthorized("The account is unavailable."));
    Session replacement = newSession(user, current.getFamilyId(), now);
    current.rotateTo(replacement.tokenId(), now);
    auditService.record(user.getId(), null, "USER", user.getId(), "TOKEN_REFRESHED", Map.of());
    return replacement;
  }

  /** Idempotently revokes the presented token without revealing whether it existed. */
  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByTokenHash(hash(rawToken))
        .ifPresent(
            token -> {
              if (!token.isRevoked()) {
                token.revoke(clock.instant());
              }
              auditService.record(
                  token.getUserId(), null, "USER", token.getUserId(), "LOGGED_OUT", Map.of());
            });
  }

  /** Executes the me operation for AuthService. */
  @Transactional(readOnly = true)
  public UserResponse me(UUID userId) {
    return users
        .findById(userId)
        .filter(UserEntity::isEnabled)
        .map(this::toUserResponse)
        .orElseThrow(() -> ApiExceptions.unauthorized("The account is unavailable."));
  }

  /** Persists only a digest; the raw refresh bearer leaves this service only in the session. */
  private Session newSession(UserEntity user, UUID familyId, Instant now) {
    byte[] bytes = new byte[48];
    SECURE_RANDOM.nextBytes(bytes);
    String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    RefreshTokenEntity token =
        refreshTokens.save(
            RefreshTokenEntity.create(
                user.getId(),
                familyId,
                hash(rawRefreshToken),
                now,
                now.plus(properties.refreshTokenTtl())));
    return new Session(
        token.getId(),
        rawRefreshToken,
        new TokenResponse(
            jwtService.issue(user), properties.accessTokenTtl().toSeconds(), toUserResponse(user)));
  }

  /** Maps the source data to user response. */
  private UserResponse toUserResponse(UserEntity user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getRole().name(),
        user.getCreatedAt());
  }

  /** Normalizes email for the current operation. */
  private String normalizeEmail(String email) {
    return email.strip().toLowerCase(java.util.Locale.ROOT);
  }

  /** Hashes opaque refresh tokens before lookup or persistence so rows hold no bearer secret. */
  private String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }

  public record Session(UUID tokenId, String refreshToken, TokenResponse response) {}
}
