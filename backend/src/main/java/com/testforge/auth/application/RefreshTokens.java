package com.testforge.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Creates opaque refresh bearers and irreversible lookup digests. */
final class RefreshTokens {
  private static final SecureRandom RANDOM = new SecureRandom();

  /** Creates an empty RefreshTokens instance for framework-managed construction. */
  private RefreshTokens() {}

  /** Generates structured manual test coverage from the requirement input. */
  static String generate() {
    byte[] bytes = new byte[48];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Reports whether the result h. */
  static String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
