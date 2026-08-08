package com.testforge.auth.controller;

import com.testforge.auth.application.AuthService;
import com.testforge.auth.application.AuthService.Session;
import com.testforge.auth.dto.AuthDtos.CsrfResponse;
import com.testforge.auth.dto.AuthDtos.LoginRequest;
import com.testforge.auth.dto.AuthDtos.RegisterRequest;
import com.testforge.auth.dto.AuthDtos.TokenResponse;
import com.testforge.auth.dto.AuthDtos.UserResponse;
import com.testforge.common.error.ApiExceptionHandler;
import com.testforge.config.AuthProperties;
import com.testforge.config.SecurityProperties;
import com.testforge.security.CurrentUser;
import com.testforge.security.RateLimitService;
import com.testforge.security.TrustedClientAddressResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  private final AuthProperties authProperties;
  private final SecurityProperties securityProperties;
  private final RateLimitService rateLimitService;
  private final CurrentUser currentUser;
  private final TrustedClientAddressResolver clientAddresses;

  /** Initializes AuthController with its required collaborators and domain state. */
  public AuthController(
      AuthService authService,
      AuthProperties authProperties,
      SecurityProperties securityProperties,
      RateLimitService rateLimitService,
      CurrentUser currentUser,
      TrustedClientAddressResolver clientAddresses) {
    this.authService = authService;
    this.authProperties = authProperties;
    this.securityProperties = securityProperties;
    this.rateLimitService = rateLimitService;
    this.currentUser = currentUser;
    this.clientAddresses = clientAddresses;
  }

  /** Handles the authenticated HTTP request to csrf. */
  @GetMapping("/csrf")
  CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getHeaderName(), token.getToken());
  }

  /** Handles the authenticated HTTP request to register. */
  @PostMapping("/register")
  ResponseEntity<TokenResponse> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
    checkAuthRate(
        httpRequest, "account:" + request.email().strip().toLowerCase(java.util.Locale.ROOT));
    return sessionResponse(authService.register(request), HttpStatus.CREATED);
  }

  /** Handles the authenticated HTTP request to login. */
  @PostMapping("/login")
  ResponseEntity<TokenResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    checkAuthRate(
        httpRequest, "account:" + request.email().strip().toLowerCase(java.util.Locale.ROOT));
    return sessionResponse(authService.login(request), HttpStatus.OK);
  }

  /** Handles the authenticated HTTP request to refresh. */
  @PostMapping("/refresh")
  ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
    String refreshToken = readRefreshToken(request);
    checkAuthRate(request, "refresh:" + (refreshToken == null ? "missing" : refreshToken));
    request.setAttribute(
        ApiExceptionHandler.CLEAR_COOKIE_ATTRIBUTE, expiredRefreshCookie().toString());
    return sessionResponse(authService.refresh(refreshToken), HttpStatus.OK);
  }

  /** Handles the authenticated HTTP request to logout. */
  @PostMapping("/logout")
  ResponseEntity<Void> logout(HttpServletRequest request) {
    authService.logout(readRefreshToken(request));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
        .build();
  }

  /** Handles the authenticated HTTP request to me. */
  @GetMapping("/me")
  UserResponse me(Authentication authentication) {
    return authService.me(currentUser.id(authentication));
  }

  /** Handles the authenticated HTTP request to session response. */
  private ResponseEntity<TokenResponse> sessionResponse(Session session, HttpStatus status) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
        .body(session.response());
  }

  /** Handles the authenticated HTTP request to refresh cookie. */
  private ResponseCookie refreshCookie(String token) {
    return ResponseCookie.from(authProperties.refreshCookieName(), token)
        .httpOnly(true)
        .secure(authProperties.secureCookies())
        .sameSite("Strict")
        .path("/")
        .maxAge(authProperties.refreshTokenTtl())
        .build();
  }

  /** Handles the authenticated HTTP request to expired refresh cookie. */
  private ResponseCookie expiredRefreshCookie() {
    return ResponseCookie.from(authProperties.refreshCookieName(), "")
        .httpOnly(true)
        .secure(authProperties.secureCookies())
        .sameSite("Strict")
        .path("/")
        .maxAge(0)
        .build();
  }

  /** Handles the authenticated HTTP request to read refresh token. */
  private String readRefreshToken(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    return Arrays.stream(request.getCookies())
        .filter(cookie -> authProperties.refreshCookieName().equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

  /** Handles the authenticated HTTP request to check auth rate. */
  private void checkAuthRate(HttpServletRequest request, String sensitiveSubject) {
    rateLimitService.check(
        "auth-client",
        clientAddresses.resolve(request),
        securityProperties.authAttemptsPerMinute());
    rateLimitService.checkDigest(
        "auth-subject", sensitiveSubject, securityProperties.authAttemptsPerMinute());
  }
}
