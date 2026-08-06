package com.testforge.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.testforge.common.correlation.CorrelationIdFilter;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties({
  AuthProperties.class,
  SecurityProperties.class,
  GenerationProperties.class,
  OpenAiProperties.class,
  DemoProperties.class
})
public class SecurityConfiguration {

  private static final String AUDIENCE = "testforge-api";
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

  /** Creates the Spring-managed clock component. */
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /** Creates the Spring-managed password encoder component. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  /** Rejects configured HMAC keys below the 32-byte minimum required for HS256. */
  @Bean
  SecretKey accessTokenKey(AuthProperties properties) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(properties.accessTokenSecret());
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("JWT_ACCESS_TOKEN_SECRET must be valid Base64.", exception);
    }
    if (decoded.length < 32) {
      throw new IllegalStateException("JWT_ACCESS_TOKEN_SECRET must contain at least 32 bytes.");
    }
    return new SecretKeySpec(decoded, "HmacSHA256");
  }

  /** Creates the Spring-managed jwt encoder component. */
  @Bean
  JwtEncoder jwtEncoder(SecretKey accessTokenKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenKey));
  }

  /** Requires signature, timestamp, issuer, and API audience before accepting a bearer token. */
  @Bean
  JwtDecoder jwtDecoder(SecretKey accessTokenKey, AuthProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(accessTokenKey).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> issuer =
        new JwtClaimValidator<>("iss", claim -> properties.issuer().equals(String.valueOf(claim)));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), issuer, new JwtAudienceValidator(AUDIENCE)));
    return decoder;
  }

  /**
   * Keeps auth entry points public while CSRF, exact CORS, and stateless JWT checks stay active.
   */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtDecoder jwtDecoder,
      CorrelationIdFilter correlationIdFilter,
      CorsConfigurationSource corsConfigurationSource,
      AuthProperties authProperties)
      throws Exception {
    CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfRepository.setCookieCustomizer(
        cookie ->
            cookie.secure(authProperties.secureCookies()).sameSite("Strict").path("/").maxAge(-1));
    CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
    csrfHandler.setCsrfRequestAttributeName("_csrf");

    return http.authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/health",
                        "/api/v1/auth/csrf",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfHandler))
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)))
        .requestCache(requestCache -> requestCache.disable())
        .securityContext(securityContext -> securityContext.requireExplicitSave(true))
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        policy -> policy.policyDirectives(CONTENT_SECURITY_POLICY))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(
                        permissions ->
                            permissions.policy("camera=(), microphone=(), geolocation=()")))
        .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /** Restricts credentialed browser calls to configured origins and a fixed explicit header set. */
  @Bean
  CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        java.util.List.of(
            "Authorization",
            "Content-Type",
            "Idempotency-Key",
            "If-Match",
            "X-XSRF-TOKEN",
            "X-Correlation-ID"));
    configuration.setExposedHeaders(java.util.List.of("X-Correlation-ID", "Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
