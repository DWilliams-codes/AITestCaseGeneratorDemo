package com.testforge.requirement.controller;

import com.testforge.common.dto.PageResponse;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.dto.RequirementDtos.AcceptanceCriterionRequest;
import com.testforge.requirement.dto.RequirementDtos.AcceptanceCriterionResponse;
import com.testforge.requirement.dto.RequirementDtos.AmbiguityResponse;
import com.testforge.requirement.dto.RequirementDtos.CreateRequirementRequest;
import com.testforge.requirement.dto.RequirementDtos.RequirementResponse;
import com.testforge.requirement.dto.RequirementDtos.RequirementSummaryResponse;
import com.testforge.requirement.dto.RequirementDtos.ResolveAmbiguityRequest;
import com.testforge.requirement.dto.RequirementDtos.UpdateRequirementRequest;
import com.testforge.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class RequirementController {
  private final RequirementService requirementService;
  private final CurrentUser currentUser;

  public RequirementController(RequirementService requirementService, CurrentUser currentUser) {
    this.requirementService = requirementService;
    this.currentUser = currentUser;
  }

  @GetMapping("/projects/{projectId}/requirements")
  PageResponse<RequirementSummaryResponse> list(
      Authentication authentication,
      @PathVariable UUID projectId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return requirementService.list(currentUser.id(authentication), projectId, page, size);
  }

  @PostMapping("/projects/{projectId}/requirements")
  @ResponseStatus(HttpStatus.CREATED)
  RequirementResponse create(
      Authentication authentication,
      @PathVariable UUID projectId,
      @Valid @RequestBody CreateRequirementRequest request) {
    return requirementService.create(currentUser.id(authentication), projectId, request);
  }

  @GetMapping("/requirements/{requirementId}")
  RequirementResponse get(Authentication authentication, @PathVariable UUID requirementId) {
    return requirementService.get(currentUser.id(authentication), requirementId);
  }

  @PatchMapping("/requirements/{requirementId}")
  RequirementResponse update(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @Valid @RequestBody UpdateRequirementRequest request) {
    return requirementService.update(currentUser.id(authentication), requirementId, request);
  }

  @PostMapping("/requirements/{requirementId}/acceptance-criteria")
  @ResponseStatus(HttpStatus.CREATED)
  AcceptanceCriterionResponse addCriterion(
      Authentication authentication,
      @PathVariable UUID requirementId,
      @Valid @RequestBody AcceptanceCriterionRequest request) {
    return requirementService.addCriterion(currentUser.id(authentication), requirementId, request);
  }

  @PatchMapping("/acceptance-criteria/{criterionId}")
  AcceptanceCriterionResponse updateCriterion(
      Authentication authentication,
      @PathVariable UUID criterionId,
      @Valid @RequestBody AcceptanceCriterionRequest request) {
    return requirementService.updateCriterion(currentUser.id(authentication), criterionId, request);
  }

  @DeleteMapping("/acceptance-criteria/{criterionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteCriterion(Authentication authentication, @PathVariable UUID criterionId) {
    requirementService.deleteCriterion(currentUser.id(authentication), criterionId);
  }

  @PostMapping("/ambiguities/{ambiguityId}/resolve")
  AmbiguityResponse resolveAmbiguity(
      Authentication authentication,
      @PathVariable UUID ambiguityId,
      @Valid @RequestBody ResolveAmbiguityRequest request) {
    return requirementService.resolveAmbiguity(
        currentUser.id(authentication), ambiguityId, request);
  }
}
