package com.testforge.config;

import com.testforge.user.repository.UserRepository;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes only the deliberately public disposable credentials after safe startup. */
@RestController
public class DemoInfoController {
  private final DemoModePolicy policy;
  private final UserRepository users;
  private final PasswordEncoder encoder;

  /** Keeps metadata tied to server-validated mode and persisted account readiness. */
  public DemoInfoController(DemoModePolicy policy, UserRepository users, PasswordEncoder encoder) {
    this.policy = policy;
    this.users = users;
    this.encoder = encoder;
  }

  /** Never exposes configuration values or credentials for a normal application account. */
  @GetMapping("/api/v1/demo-info")
  public Map<String, Object> get() {
    if (!policy.isReady()
        || users
            .findByEmailNormalized(DemoDataSeeder.DEMO_EMAIL)
            .filter(
                user ->
                    user.isEnabled()
                        && encoder.matches(DemoDataSeeder.DEMO_PASSWORD, user.getPasswordHash()))
            .isEmpty()) {
      return Map.of("enabled", false);
    }
    return Map.of(
        "enabled",
        true,
        "email",
        DemoDataSeeder.DEMO_EMAIL,
        "password",
        DemoDataSeeder.DEMO_PASSWORD,
        "label",
        DemoModePolicy.FIXTURE_LABEL);
  }
}
