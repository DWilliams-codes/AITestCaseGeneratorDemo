package com.testforge.config;

import com.testforge.common.error.ApiExceptions;
import com.testforge.user.repository.UserRepository;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Isolates the public disposable account from all real provider configurations. */
@Component
public final class DemoModePolicy {
  public static final String FIXTURE_URL = "http://127.0.0.1:8081/v1";
  public static final String FIXTURE_KEY = "synthetic-e2e-only";
  public static final String FIXTURE_MODEL = "testforge-review-fixture";
  public static final String FIXTURE_LABEL =
      "Local fixture demo: maintained synthetic responses; no live AI call.";
  private final boolean enabled;
  private final UserRepository users;
  private volatile boolean ready;

  /** Validates the entire fixture boundary at startup, before seeding or provider use. */
  public DemoModePolicy(
      DemoProperties demo,
      GenerationProperties generation,
      OpenAiProperties provider,
      Environment environment,
      UserRepository users) {
    this.users = users;
    enabled = demo.fixtureMode() && demo.seedEnabled();
    if (!demo.fixtureMode() && !demo.seedEnabled()) return;
    var profiles = Arrays.asList(environment.getActiveProfiles());
    if (!enabled
        || !profiles.contains("local")
        || profiles.stream().anyMatch(p -> !p.equals("local"))
        || !"openai".equals(generation.provider())
        || !FIXTURE_URL.equals(provider.baseUrl())
        || !FIXTURE_KEY.equals(provider.apiKey())
        || !"127.0.0.1".equals(environment.getProperty("server.address"))) {
      throw new IllegalStateException(
          "Public demo requires fixture-mode and seed-enabled, local profile only, loopback server binding, and the exact loopback fixture URL and public synthetic key. Disable both demo flags for normal use.");
    }
  }

  /** Reports only configuration validated by the startup boundary. */
  public boolean isEnabled() {
    return enabled;
  }

  /** Publishes readiness only after the disposable account and source seed are verified. */
  public void markReady() {
    if (enabled) ready = true;
  }

  /** Keeps credential metadata unavailable until safe seeding completes. */
  public boolean isReady() {
    return enabled && ready;
  }

  /** Reserves the known public principal exclusively for the isolated fixture mode. */
  public boolean isPrincipalAllowed(String email) {
    return enabled || !DemoDataSeeder.DEMO_EMAIL.equalsIgnoreCase(email.strip());
  }

  /** Uses generic authentication failure so reserved-account state is not disclosed. */
  public void requirePrincipalAllowed(String email) {
    if (!isPrincipalAllowed(email)) throw ApiExceptions.unauthorized("The account is unavailable.");
  }

  /** Rechecks persisted identity before claiming work, including tokens issued in an older mode. */
  public void requireGenerationAllowed(UUID userId) {
    users.findById(userId).ifPresent(user -> requirePrincipalAllowed(user.getEmail()));
  }
}
