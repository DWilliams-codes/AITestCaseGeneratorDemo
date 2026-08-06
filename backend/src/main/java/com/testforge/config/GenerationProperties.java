package com.testforge.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("testforge.generation")
public record GenerationProperties(
    String provider, int maximumCases, int maximumStepsPerCase, Duration stalePendingTimeout) {
  /** Prevents instantiation because GenerationProperties is a static utility namespace. */
  public GenerationProperties {
    if (maximumCases < 1 || maximumCases > 25) {
      throw new IllegalArgumentException("maximum-cases must be between 1 and 25");
    }
    if (maximumStepsPerCase < 1 || maximumStepsPerCase > 30) {
      throw new IllegalArgumentException("maximum-steps-per-case must be between 1 and 30");
    }
    if (stalePendingTimeout == null
        || stalePendingTimeout.isNegative()
        || stalePendingTimeout.isZero()) {
      throw new IllegalArgumentException("stale-pending-timeout must be positive");
    }
  }
}
