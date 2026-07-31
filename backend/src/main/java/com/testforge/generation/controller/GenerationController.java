package com.testforge.generation.controller;

import com.testforge.config.SecurityProperties;
import com.testforge.generation.application.GenerationService;
import com.testforge.generation.dto.GenerationRunResponse;
import com.testforge.security.CurrentUser;
import com.testforge.security.RateLimitService;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class GenerationController {
  private final GenerationService generationService;
  private final CurrentUser currentUser;
  private final RateLimitService rateLimitService;
  private final SecurityProperties securityProperties;

  public GenerationController(
      GenerationService generationService,
      CurrentUser currentUser,
      RateLimitService rateLimitService,
      SecurityProperties securityProperties) {
    this.generationService = generationService;
    this.currentUser = currentUser;
    this.rateLimitService = rateLimitService;
    this.securityProperties = securityProperties;
  }

  @PostMapping({
    "/requirements/{requirementId}/generate-test-cases",
    "/requirements/{requirementId}/regenerate"
  })
  @ResponseStatus(HttpStatus.CREATED)
  GenerationRunResponse generate(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestHeader("Idempotency-Key") @Size(min = 8, max = 200) String idempotencyKey) {
    UUID userId = currentUser.id(authentication);
    rateLimitService.check(
        "generation", userId.toString(), securityProperties.generationAttemptsPerMinute());
    return generationService.generate(userId, requirementId, idempotencyKey);
  }

  @GetMapping("/generation-runs/{runId}")
  GenerationRunResponse get(Authentication authentication, @PathVariable UUID runId) {
    return generationService.get(currentUser.id(authentication), runId);
  }
}
