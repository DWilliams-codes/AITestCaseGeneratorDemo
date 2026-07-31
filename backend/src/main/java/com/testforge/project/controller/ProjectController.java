package com.testforge.project.controller;

import com.testforge.common.dto.PageResponse;
import com.testforge.project.application.ProjectService;
import com.testforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.testforge.project.dto.ProjectDtos.ProjectResponse;
import com.testforge.project.dto.ProjectDtos.UpdateProjectRequest;
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
@RequestMapping("/api/v1/projects")
public class ProjectController {
  private final ProjectService projectService;
  private final CurrentUser currentUser;

  /** Initializes ProjectController with its required collaborators and domain state. */
  public ProjectController(ProjectService projectService, CurrentUser currentUser) {
    this.projectService = projectService;
    this.currentUser = currentUser;
  }

  /** Handles the authenticated HTTP request to list. */
  @GetMapping
  PageResponse<ProjectResponse> list(
      Authentication authentication,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return projectService.list(currentUser.id(authentication), page, size);
  }

  /** Handles the authenticated HTTP request to create. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ProjectResponse create(
      Authentication authentication, @Valid @RequestBody CreateProjectRequest request) {
    return projectService.create(currentUser.id(authentication), request);
  }

  /** Handles the authenticated HTTP request to get. */
  @GetMapping("/{projectId}")
  ProjectResponse get(Authentication authentication, @PathVariable UUID projectId) {
    return projectService.get(currentUser.id(authentication), projectId);
  }

  /** Handles the authenticated HTTP request to update. */
  @PatchMapping("/{projectId}")
  ProjectResponse update(
      Authentication authentication,
      @PathVariable UUID projectId,
      @Valid @RequestBody UpdateProjectRequest request) {
    return projectService.update(currentUser.id(authentication), projectId, request);
  }

  /** Handles the authenticated HTTP request to archive. */
  @DeleteMapping("/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void archive(Authentication authentication, @PathVariable UUID projectId) {
    projectService.archive(currentUser.id(authentication), projectId);
  }
}
