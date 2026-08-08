package com.testforge.security;

import com.testforge.common.error.ApiExceptions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Per-instance abuse brake; horizontally scaled deployments still require a shared limiter. */
@Service
public class RateLimitService {
  static final int MAX_ACTIVE_WINDOWS = 10_000;
  private final Clock clock;
  private final Map<String, Integer> windows = new HashMap<>();
  private final Object admissionLock = new Object();
  private Instant activeMinute;

  /** Initializes RateLimitService with its required collaborators and domain state. */
  public RateLimitService(Clock clock) {
    this.clock = clock;
  }

  /** Isolates counts by bucket and stable subject; this limit is not an authorization control. */
  public void check(String bucket, String subject, int limit) {
    String key = bucket + ':' + subject;
    int current;
    synchronized (admissionLock) {
      Instant minute = clock.instant().truncatedTo(ChronoUnit.MINUTES);
      if (activeMinute == null || minute.isAfter(activeMinute)) {
        windows.clear();
        activeMinute = minute;
      }
      Integer prior = windows.get(key);
      if (prior == null && windows.size() >= MAX_ACTIVE_WINDOWS) {
        throw ApiExceptions.tooManyRequests("Too many requests. Try again in one minute.");
      }
      current = prior == null ? 1 : prior + 1;
      windows.put(key, current);
    }
    if (current > limit) {
      throw ApiExceptions.tooManyRequests("Too many requests. Try again in one minute.");
    }
  }

  /** Exposes the bounded in-memory cardinality to package-local deterministic tests. */
  int trackedWindowCount() {
    synchronized (admissionLock) {
      return windows.size();
    }
  }

  /** Uses only a bounded digest when the rate-limit subject may contain credentials or identity. */
  public void checkDigest(String bucket, String subject, int limit) {
    String safeSubject = subject == null ? "missing" : subject;
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(safeSubject.getBytes(StandardCharsets.UTF_8));
      check(bucket, java.util.HexFormat.of().formatHex(digest), limit);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
