package com.testforge.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.testforge.user.repository.UserRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DemoModePolicyTest {
  /** Covers every required fixture boundary and rejects alternate endpoint spellings. */
  @Test
  void validatesGateMatrix() {
    assertThat(
            policy(
                    true,
                    true,
                    "local",
                    DemoModePolicy.FIXTURE_URL,
                    DemoModePolicy.FIXTURE_KEY,
                    "127.0.0.1")
                .isEnabled())
        .isTrue();
    for (String url :
        new String[] {
          "http://localhost:8081/v1",
          "https://127.0.0.1:8081/v1",
          "http://127.0.0.1:8081/v1?x=1",
          "http://127.0.0.1:8081/v1#x",
          "http://user@127.0.0.1:8081/v1",
          "http://127.0.0.1:8082/v1",
          "https://api.openai.com/v1"
        }) {
      assertThatThrownBy(
              () -> policy(true, true, "local", url, DemoModePolicy.FIXTURE_KEY, "127.0.0.1"))
          .isInstanceOf(IllegalStateException.class);
    }
    for (String profile : new String[] {"", "prod", "production", "local,production", "test"}) {
      assertThatThrownBy(
              () ->
                  policy(
                      true,
                      true,
                      profile,
                      DemoModePolicy.FIXTURE_URL,
                      DemoModePolicy.FIXTURE_KEY,
                      "127.0.0.1"))
          .isInstanceOf(IllegalStateException.class);
    }
    assertThatThrownBy(
            () ->
                policy(
                    true,
                    false,
                    "local",
                    DemoModePolicy.FIXTURE_URL,
                    DemoModePolicy.FIXTURE_KEY,
                    "127.0.0.1"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                policy(
                    false,
                    true,
                    "local",
                    DemoModePolicy.FIXTURE_URL,
                    DemoModePolicy.FIXTURE_KEY,
                    "127.0.0.1"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                policy(
                    true,
                    true,
                    "local",
                    DemoModePolicy.FIXTURE_URL,
                    "not-the-public-sentinel",
                    "127.0.0.1"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                policy(
                    true,
                    true,
                    "local",
                    DemoModePolicy.FIXTURE_URL,
                    DemoModePolicy.FIXTURE_KEY,
                    "0.0.0.0"))
        .isInstanceOf(IllegalStateException.class);
  }

  /** Keeps normal configuration unchanged while reserving the public principal and readiness. */
  @Test
  void disabledModeCannotPublishOrAuthenticatePublicPrincipal() {
    var disabled =
        policy(false, false, "prod", "https://api.openai.com/v1", "unresolved", "0.0.0.0");
    disabled.markReady();
    assertThat(disabled.isReady()).isFalse();
    assertThat(disabled.isPrincipalAllowed(" DEMO@testforge.local ")).isFalse();
    assertThat(disabled.isPrincipalAllowed("ordinary@example.test")).isTrue();
    assertThatThrownBy(() -> disabled.requirePrincipalAllowed(DemoDataSeeder.DEMO_EMAIL))
        .isInstanceOf(com.testforge.common.error.ApiException.class);
    var safe =
        policy(
            true,
            true,
            "local",
            DemoModePolicy.FIXTURE_URL,
            DemoModePolicy.FIXTURE_KEY,
            "127.0.0.1");
    assertThat(safe.isReady()).isFalse();
    safe.markReady();
    assertThat(safe.isReady()).isTrue();
  }

  /** Builds isolated configuration with no network or secret resolution. */
  private DemoModePolicy policy(
      boolean fixture, boolean seed, String profile, String url, String key, String bind) {
    var environment = new MockEnvironment().withProperty("server.address", bind);
    if (!profile.isEmpty()) environment.setActiveProfiles(profile.split(","));
    return new DemoModePolicy(
        new DemoProperties(seed, fixture),
        new GenerationProperties("openai", 25, 30, Duration.ofMinutes(4)),
        new OpenAiProperties(key, url, "unused", 1, 1, 4000),
        environment,
        mock(UserRepository.class));
  }
}
