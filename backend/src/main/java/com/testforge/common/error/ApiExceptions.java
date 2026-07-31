package com.testforge.common.error;

import org.springframework.http.HttpStatus;

public final class ApiExceptions {
  /** Creates an empty ApiExceptions instance for framework-managed construction. */
  private ApiExceptions() {}

  /** Executes the not found operation for ApiExceptions. */
  public static ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", message);
  }

  /** Executes the conflict operation for ApiExceptions. */
  public static ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }

  /** Executes the unauthorized operation for ApiExceptions. */
  public static ApiException unauthorized(String message) {
    return new ApiException(HttpStatus.UNAUTHORIZED, "authentication_failed", message);
  }

  /** Executes the bad request operation for ApiExceptions. */
  public static ApiException badRequest(String code, String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, message);
  }

  /** Maps the source data to o many requests. */
  public static ApiException tooManyRequests(String message) {
    return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", message);
  }
}
