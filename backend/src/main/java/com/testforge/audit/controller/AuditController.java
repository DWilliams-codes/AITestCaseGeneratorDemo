package com.testforge.audit.controller;

import com.testforge.audit.dto.AuditEventResponse;
import com.testforge.audit.repository.AuditEventRepository;
import com.testforge.common.dto.PageResponse;
import com.testforge.project.application.ProjectService;
import com.testforge.security.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/projects/{projectId}/audit-events")
public class AuditController {
  private final AuditEventRepository events;
  private final ProjectService projectService;
  private final CurrentUser currentUser;

  /** Initializes AuditController with its required collaborators and domain state. */
  public AuditController(
      AuditEventRepository events, ProjectService projectService, CurrentUser currentUser) {
    this.events = events;
    this.projectService = projectService;
    this.currentUser = currentUser;
  }

  /** Handles the authenticated HTTP request to list. */
  @GetMapping
  @Transactional(readOnly = true)
  public PageResponse<AuditEventResponse> list(
      Authentication authentication,
      @PathVariable UUID projectId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    projectService.requireOwned(currentUser.id(authentication), projectId);
    return PageResponse.from(
        events
            .findAllByProjectIdOrderByTimestampDesc(projectId, PageRequest.of(page, size))
            .map(
                event ->
                    new AuditEventResponse(
                        event.getId(),
                        event.getActorId(),
                        event.getProjectId(),
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getAction(),
                        event.getMetadata(),
                        event.getTimestamp(),
                        event.getCorrelationId())));
  }
}
