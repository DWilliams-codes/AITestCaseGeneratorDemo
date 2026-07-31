package com.testforge.security;

import com.testforge.common.error.ApiExceptions;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  /** Executes the id operation for CurrentUser. */
  public UUID id(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw ApiExceptions.unauthorized("Authentication is required.");
    }
    try {
      return UUID.fromString(token.getToken().getSubject());
    } catch (IllegalArgumentException exception) {
      throw ApiExceptions.unauthorized("The access token subject is invalid.");
    }
  }
}
