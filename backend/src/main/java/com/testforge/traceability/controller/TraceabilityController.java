package com.testforge.traceability.controller;

import com.testforge.security.CurrentUser;
import com.testforge.traceability.application.TraceabilityService;
import com.testforge.traceability.dto.TraceabilityDtos.CoverageResponse;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/user-stories/{requirementId}", "/api/v1/requirements/{requirementId}"})
public class TraceabilityController {
  private final TraceabilityService traceabilityService;
  private final CurrentUser currentUser;

  /** Initializes TraceabilityController with its required collaborators and domain state. */
  public TraceabilityController(TraceabilityService traceabilityService, CurrentUser currentUser) {
    this.traceabilityService = traceabilityService;
    this.currentUser = currentUser;
  }

  /** Handles the authenticated HTTP request to traceability. */
  @GetMapping("/traceability")
  TraceabilityResponse traceability(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestParam(required = false) UUID generationRunId) {
    return traceabilityService.traceability(
        currentUser.id(authentication), requirementId, generationRunId);
  }

  /** Handles the authenticated HTTP request to coverage. */
  @GetMapping("/coverage")
  CoverageResponse coverage(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @RequestParam(required = false) UUID generationRunId) {
    return traceabilityService.coverage(
        currentUser.id(authentication), requirementId, generationRunId);
  }
}
