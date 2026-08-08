package com.testforge.common.error;

import com.testforge.common.correlation.CorrelationIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
  public static final String CLEAR_COOKIE_ATTRIBUTE =
      ApiExceptionHandler.class.getName() + ".clearCookie";

  private final Clock clock;

  /** Initializes ApiExceptionHandler with its required collaborators and domain state. */
  public ApiExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Handles api exception for the current operation. */
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> handleApiException(
      ApiException exception, HttpServletRequest request) {
    ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
    Object clearCookie = request.getAttribute(CLEAR_COOKIE_ATTRIBUTE);
    if (clearCookie instanceof String cookie) {
      response.header(HttpHeaders.SET_COOKIE, cookie);
    }
    return response.body(
        problem(
            exception.getStatus(),
            exception.getCode(),
            exception.getMessage(),
            request.getRequestURI()));
  }

  /** Handles validation for the current operation. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    ProblemDetail detail =
        problem(
            HttpStatus.BAD_REQUEST,
            "validation_failed",
            "One or more request fields are invalid.",
            request.getRequestURI());
    Map<String, String> errors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    detail.setProperty("errors", errors);
    return detail;
  }

  /** Maps validated headers, paths, and query values to the bounded client error contract. */
  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintValidation(
      ConstraintViolationException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "validation_failed",
        "One or more request values are invalid.",
        request.getRequestURI());
  }

  /** Handles unreadable for the current operation. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "malformed_request",
        "The request body is malformed or contains unsupported values.",
        request.getRequestURI());
  }

  /** Handles optimistic lock for the current operation. */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(
      OptimisticLockingFailureException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "stale_version",
        "This resource changed since it was loaded. Refresh and retry your update.",
        request.getRequestURI());
  }

  /** Executes the problem operation for ApiExceptionHandler. */
  private ProblemDetail problem(HttpStatus status, String code, String detail, String requestPath) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(URI.create("https://testforge.local/problems/" + code));
    problem.setInstance(URI.create(requestPath));
    problem.setProperty("code", code);
    problem.setProperty("correlationId", CorrelationIds.current());
    problem.setProperty("timestamp", clock.instant());
    return problem;
  }
}
