package com.testforge.common.error;

import com.testforge.common.correlation.CorrelationIds;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
    return problem(
        exception.getStatus(),
        exception.getCode(),
        exception.getMessage(),
        request.getRequestURI());
  }

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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "malformed_request",
        "The request body is malformed or contains unsupported values.",
        request.getRequestURI());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(
      OptimisticLockingFailureException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "stale_version",
        "This resource changed since it was loaded. Refresh and retry your update.",
        request.getRequestURI());
  }

  private ProblemDetail problem(HttpStatus status, String code, String detail, String requestPath) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(URI.create("https://testforge.local/problems/" + code));
    problem.setInstance(URI.create(requestPath));
    problem.setProperty("code", code);
    problem.setProperty("correlationId", CorrelationIds.current());
    return problem;
  }
}
