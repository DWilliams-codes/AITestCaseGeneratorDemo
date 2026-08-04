package com.testforge.project.application;

import com.testforge.audit.application.AuditService;
import com.testforge.common.dto.PageResponse;
import com.testforge.common.error.ApiExceptions;
import com.testforge.project.domain.ProjectEntity;
import com.testforge.project.dto.ProjectDtos.CreateProjectRequest;
import com.testforge.project.dto.ProjectDtos.ProjectResponse;
import com.testforge.project.dto.ProjectDtos.UpdateProjectRequest;
import com.testforge.project.repository.ProjectRepository;
import com.testforge.requirement.repository.RequirementRepository;
import com.testforge.workspace.application.WorkspaceService;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final RequirementRepository requirements;
  private final AuditService auditService;
  private final Clock clock;
  private final WorkspaceService workspaceService;

  /** Initializes ProjectService with its required collaborators and domain state. */
  public ProjectService(
      ProjectRepository projects,
      RequirementRepository requirements,
      AuditService auditService,
      Clock clock,
      WorkspaceService workspaceService) {
    this.projects = projects;
    this.requirements = requirements;
    this.auditService = auditService;
    this.clock = clock;
    this.workspaceService = workspaceService;
  }

  /** Lists resources visible to the current owner using the requested page. */
  @Transactional(readOnly = true)
  public PageResponse<ProjectResponse> list(UUID ownerId, int page, int size) {
    return PageResponse.from(
        projects
            .findAllByOwnerIdOrderByUpdatedAtDesc(ownerId, PageRequest.of(page, size))
            .map(this::toResponse));
  }

  /** Returns the owned resource identified by the request. */
  @Transactional(readOnly = true)
  public ProjectResponse get(UUID ownerId, UUID projectId) {
    return toResponse(requireOwned(ownerId, projectId));
  }

  /** Creates and persists a new domain resource from validated input. */
  @Transactional
  public ProjectResponse create(UUID ownerId, CreateProjectRequest request) {
    UUID workspaceId = workspaceService.requirePersonalWorkspaceId(ownerId);
    ProjectEntity project =
        projects.save(
            ProjectEntity.create(
                ownerId,
                workspaceId,
                request.name().strip(),
                clean(request.description()),
                clock.instant()));
    auditService.record(ownerId, project.getId(), "PROJECT", project.getId(), "CREATED", Map.of());
    return toResponse(project);
  }

  /** Applies a validated update while preserving concurrency guarantees. */
  @Transactional
  public ProjectResponse update(UUID ownerId, UUID projectId, UpdateProjectRequest request) {
    ProjectEntity project = requireOwned(ownerId, projectId);
    assertVersion(project.getVersion(), request.version());
    project.update(request.name().strip(), clean(request.description()), clock.instant());
    auditService.record(ownerId, projectId, "PROJECT", projectId, "UPDATED", Map.of());
    return toResponse(project);
  }

  /** Archives the owned resource and records the state transition. */
  @Transactional
  public void archive(UUID ownerId, UUID projectId) {
    ProjectEntity project = requireOwned(ownerId, projectId);
    project.archive(clock.instant());
    auditService.record(ownerId, projectId, "PROJECT", projectId, "ARCHIVED", Map.of());
  }

  /** Loads the requested resource and verifies that it belongs to the current owner. */
  @Transactional(readOnly = true)
  public ProjectEntity requireOwned(UUID ownerId, UUID projectId) {
    return projects
        .findByIdAndOwnerId(projectId, ownerId)
        .orElseThrow(() -> ApiExceptions.notFound("Project not found."));
  }

  /** Maps the source data to response. */
  private ProjectResponse toResponse(ProjectEntity project) {
    return new ProjectResponse(
        project.getId(),
        project.getWorkspaceId(),
        project.getName(),
        project.getDescription(),
        project.getStatus(),
        requirements.countByProjectId(project.getId()),
        project.getCreatedAt(),
        project.getUpdatedAt(),
        project.getVersion());
  }

  /** Normalizes optional text before it is compared or persisted. */
  private String clean(String value) {
    return value == null ? "" : value.strip();
  }

  /** Rejects stale updates by comparing the submitted and persisted entity versions. */
  private void assertVersion(long actual, long requested) {
    if (actual != requested) {
      throw ApiExceptions.conflict(
          "stale_version", "This project changed since it was loaded. Refresh and retry.");
    }
  }
}
