package com.testforge.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("testforge.auth")
public record AuthProperties(
    String issuer,
    String accessTokenSecret,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    boolean secureCookies,
    String refreshCookieName) {}
