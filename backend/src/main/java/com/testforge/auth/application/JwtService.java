package com.testforge.auth.application;

import com.testforge.config.AuthProperties;
import com.testforge.user.domain.UserEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtEncoder encoder;
  private final AuthProperties properties;
  private final Clock clock;

  public JwtService(JwtEncoder encoder, AuthProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String issue(UserEntity user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTokenTtl()))
            .subject(user.getId().toString())
            .audience(List.of("testforge-api"))
            .claim("roles", List.of(user.getRole().name()))
            .claim("displayName", user.getDisplayName())
            .build();
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims))
        .getTokenValue();
  }
}
