package com.testforge.common.controller;

import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {
  /** Handles the authenticated HTTP request to health. */
  @GetMapping
  public HealthResponse health() {
    return new HealthResponse("UP", "testforge-backend", Instant.now());
  }

  public record HealthResponse(String status, String service, Instant timestamp) {}
}
