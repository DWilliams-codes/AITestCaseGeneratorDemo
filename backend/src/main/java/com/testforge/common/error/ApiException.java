package com.testforge.common.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  /** Initializes ApiException with its required collaborators and domain state. */
  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  /** Returns the current status value. */
  public HttpStatus getStatus() {
    return status;
  }

  /** Returns the current code value. */
  public String getCode() {
    return code;
  }
}
