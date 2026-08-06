package com.testforge.generation.controller;

import com.testforge.config.SecurityProperties;
import com.testforge.generation.application.GenerationService;
import com.testforge.generation.dto.GenerationRunPageResponse;
import com.testforge.generation.dto.GenerationRunResponse;
import com.testforge.security.CurrentUser;
import com.testforge.security.RateLimitService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  /** Initializes GenerationController with its required collaborators and domain state. */
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

  /** Handles the authenticated HTTP request to generate. */
  @PostMapping({
    "/user-stories/{requirementId}/generate-test-cases",
    "/requirements/{requirementId}/generate-test-cases"
  })
  @ResponseStatus(HttpStatus.CREATED)
  GenerationRunResponse generate(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(min = 8, max = 200) String idempotencyKey) {
    UUID userId = currentUser.id(authentication);
    rateLimitService.check(
        "generation", userId.toString(), securityProperties.generationAttemptsPerMinute());
    return generationService.generate(userId, requirementId, idempotencyKey);
  }

  /** Regenerates a new immutable set, optionally confirming supersession evidence. */
  @PostMapping({
    "/user-stories/{requirementId}/regenerate",
    "/requirements/{requirementId}/regenerate"
  })
  @ResponseStatus(HttpStatus.CREATED)
  GenerationRunResponse regenerate(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(min = 8, max = 200) String idempotencyKey,
      @RequestParam(defaultValue = "false") boolean confirmSupersede) {
    UUID userId = currentUser.id(authentication);
    rateLimitService.check(
        "generation", userId.toString(), securityProperties.generationAttemptsPerMinute());
    return generationService.regenerate(userId, requirementId, idempotencyKey, confirmSupersede);
  }

  /** Lists generation attempts and stable successful-set metadata. */
  @GetMapping({
    "/user-stories/{requirementId}/generation-runs",
    "/requirements/{requirementId}/generation-runs"
  })
  List<GenerationRunResponse> list(
      Authentication authentication, @PathVariable UUID requirementId) {
    return generationService.list(currentUser.id(authentication), requirementId);
  }

  /** Returns a canonical bounded generation-history page. */
  @GetMapping({
    "/user-stories/{requirementId}/generation-runs/page",
    "/requirements/{requirementId}/generation-runs/page"
  })
  GenerationRunPageResponse listPage(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return generationService.listPage(currentUser.id(authentication), requirementId, page, size);
  }

  /** Handles the authenticated HTTP request to get. */
  @GetMapping("/generation-runs/{runId}")
  GenerationRunResponse get(Authentication authentication, @PathVariable UUID runId) {
    return generationService.get(currentUser.id(authentication), runId);
  }
}
