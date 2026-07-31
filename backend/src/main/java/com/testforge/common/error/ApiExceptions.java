package com.testforge.common.error;

import org.springframework.http.HttpStatus;

public final class ApiExceptions {
  private ApiExceptions() {}

  public static ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, "resource_not_found", message);
  }

  public static ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }

  public static ApiException unauthorized(String message) {
    return new ApiException(HttpStatus.UNAUTHORIZED, "authentication_failed", message);
  }

  public static ApiException badRequest(String code, String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, message);
  }

  public static ApiException tooManyRequests(String message) {
    return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", message);
  }
}
