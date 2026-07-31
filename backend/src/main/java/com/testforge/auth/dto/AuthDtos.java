package com.testforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
  /** Prevents instantiation because AuthDtos is a static utility namespace. */
  private AuthDtos() {}

  public record RegisterRequest(
      @NotBlank @Email @Size(max = 320) String email,
      @NotBlank @Size(min = 2, max = 120) String displayName,
      @NotBlank @Size(min = 12, max = 128) String password) {}

  public record LoginRequest(
      @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 128) String password) {}

  public record UserResponse(
      UUID id, String email, String displayName, String role, Instant createdAt) {}

  public record TokenResponse(String accessToken, long expiresInSeconds, UserResponse user) {}

  public record CsrfResponse(String headerName, String token) {}
}
