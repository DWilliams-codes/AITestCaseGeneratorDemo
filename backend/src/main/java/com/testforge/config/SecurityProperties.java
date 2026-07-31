package com.testforge.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("testforge.security")
public record SecurityProperties(
    List<String> allowedOrigins, int authAttemptsPerMinute, int generationAttemptsPerMinute) {
  /** Prevents instantiation because SecurityProperties is a static utility namespace. */
  public SecurityProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
  }
}
