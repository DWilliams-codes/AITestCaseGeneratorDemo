package com.testforge.auth.application;

import com.testforge.audit.application.AuditMetadata;
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
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthProperties properties;
  private final Clock clock;
  private final AuditService auditService;
  private final WorkspaceService workspaceService;
  private final RefreshTokenRotationService rotationService;

  /** Initializes AuthService with its required collaborators and domain state. */
  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthProperties properties,
      Clock clock,
      AuditService auditService,
      WorkspaceService workspaceService,
      RefreshTokenRotationService rotationService) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.properties = properties;
    this.clock = clock;
    this.auditService = auditService;
    this.workspaceService = workspaceService;
    this.rotationService = rotationService;
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
    auditService.record(
        user.getId(), null, "USER", user.getId(), "REGISTERED", AuditMetadata.empty());
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
    auditService.record(
        user.getId(), null, "USER", user.getId(), "LOGGED_IN", AuditMetadata.empty());
    return newSession(user, UUID.randomUUID(), now);
  }

  /** Maps the already-committed rotation outcome to a generic authentication response. */
  public Session refresh(String rawToken) {
    RefreshTokenRotationService.RotationOutcome outcome = rotationService.rotate(rawToken);
    if (!outcome.succeeded()) {
      // Failure reasons remain server-side so callers cannot distinguish token or account state.
      throw ApiExceptions.unauthorized("The session is invalid. Sign in again.");
    }
    UserEntity user = outcome.user();
    return new Session(
        outcome.tokenId(),
        outcome.refreshToken(),
        new TokenResponse(
            jwtService.issue(user), properties.accessTokenTtl().toSeconds(), toUserResponse(user)));
  }

  /** Idempotently revokes the presented token without revealing whether it existed. */
  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByTokenHash(RefreshTokens.hash(rawToken))
        .ifPresent(
            token -> {
              if (!token.isRevoked()) {
                token.revoke(clock.instant());
              }
              auditService.record(
                  token.getUserId(),
                  null,
                  "USER",
                  token.getUserId(),
                  "LOGGED_OUT",
                  AuditMetadata.empty());
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
    String rawRefreshToken = RefreshTokens.generate();
    RefreshTokenEntity token =
        refreshTokens.save(
            RefreshTokenEntity.create(
                user.getId(),
                familyId,
                RefreshTokens.hash(rawRefreshToken),
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

  public record Session(UUID tokenId, String refreshToken, TokenResponse response) {}
}
