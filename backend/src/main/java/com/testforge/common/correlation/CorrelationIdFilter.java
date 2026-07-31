package com.testforge.common.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  /** Propagates a safe correlation identifier through the current HTTP request. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String candidate = request.getHeader(CorrelationIds.HEADER);
    String correlationId = normalize(candidate);
    MDC.put(CorrelationIds.MDC_KEY, correlationId);
    response.setHeader(CorrelationIds.HEADER, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CorrelationIds.MDC_KEY);
    }
  }

  /** Executes the normalize operation for CorrelationIdFilter. */
  private String normalize(String value) {
    if (value != null) {
      try {
        return UUID.fromString(value).toString();
      } catch (IllegalArgumentException ignored) {
        // Untrusted or malformed values are replaced at the HTTP boundary.
      }
    }
    return UUID.randomUUID().toString();
  }
}
