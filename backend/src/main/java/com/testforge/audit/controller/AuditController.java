package com.testforge.audit.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.application.AuditMetadata;
import com.testforge.audit.application.LegacyReopenAuditReconciler;
import com.testforge.audit.dto.AuditEventResponse;
import com.testforge.audit.repository.AuditEventRepository;
import com.testforge.common.dto.PageResponse;
import com.testforge.common.error.ApiExceptions;
import com.testforge.project.application.ProjectService;
import com.testforge.security.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
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
  private final ObjectMapper objectMapper;
  private final LegacyReopenAuditReconciler legacyReopens;

  /** Initializes AuditController with its required collaborators and domain state. */
  public AuditController(
      AuditEventRepository events,
      ProjectService projectService,
      CurrentUser currentUser,
      ObjectMapper objectMapper,
      LegacyReopenAuditReconciler legacyReopens) {
    this.events = events;
    this.projectService = projectService;
    this.currentUser = currentUser;
    this.objectMapper = objectMapper;
    this.legacyReopens = legacyReopens;
  }

  /** Handles the authenticated HTTP request to list. */
  @GetMapping
  @Transactional(readOnly = true)
  public PageResponse<AuditEventResponse> list(
      Authentication authentication,
      @PathVariable UUID projectId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
      @RequestParam(required = false) @Size(max = 100) String entityType,
      @RequestParam(required = false) UUID entityId,
      @RequestParam(required = false) UUID actorId,
      @RequestParam(required = false) @Size(max = 100) String action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    projectService.requireOwned(currentUser.id(authentication), projectId);
    for (int batch = 0; batch < LegacyReopenAuditReconciler.MAX_READ_BATCHES; batch++) {
      if (legacyReopens.reconcileProject(projectId) == 0) break;
    }
    if (from != null && to != null) {
      if (from.isAfter(to)) {
        throw ApiExceptions.badRequest(
            "invalid_audit_range", "Audit range start must precede end.");
      }
      if (Duration.between(from, to).toDays() > 366) {
        throw ApiExceptions.badRequest(
            "audit_range_too_large", "Audit time filters may span at most 366 days.");
      }
    }
    Specification<com.testforge.audit.domain.AuditEventEntity> filter =
        (root, query, criteria) -> criteria.equal(root.get("projectId"), projectId);
    if (entityType != null && !entityType.isBlank()) {
      String normalized = entityType.strip();
      filter =
          filter.and((root, query, criteria) -> criteria.equal(root.get("entityType"), normalized));
    }
    if (entityId != null) {
      filter =
          filter.and((root, query, criteria) -> criteria.equal(root.get("entityId"), entityId));
    }
    if (actorId != null) {
      filter = filter.and((root, query, criteria) -> criteria.equal(root.get("actorId"), actorId));
    }
    if (action != null && !action.isBlank()) {
      String normalized = action.strip();
      filter =
          filter.and((root, query, criteria) -> criteria.equal(root.get("action"), normalized));
    }
    if (from != null) {
      filter =
          filter.and(
              (root, query, criteria) ->
                  criteria.greaterThanOrEqualTo(root.get("timestamp"), from));
    }
    if (to != null) {
      filter =
          filter.and(
              (root, query, criteria) -> criteria.lessThanOrEqualTo(root.get("timestamp"), to));
    }
    return PageResponse.from(
        events
            .findAll(
                filter,
                PageRequest.of(
                    page, size, Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id"))))
            .map(
                event ->
                    new AuditEventResponse(
                        event.getId(),
                        event.getActorId(),
                        event.getProjectId(),
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getAction(),
                        structuredMetadata(event),
                        event.getTimestamp(),
                        event.getCorrelationId())));
  }

  /** Parses persisted audit metadata into structured JSON without executing content. */
  private JsonNode structuredMetadata(com.testforge.audit.domain.AuditEventEntity event) {
    String metadata = event.getMetadata();
    if ("TEST_CASE".equals(event.getEntityType())
        && "REOPENED".equals(event.getAction())
        && metadata.contains("\"reason\"")) {
      return objectMapper.valueToTree(AuditMetadata.pendingLegacyReopenRedaction().values());
    }
    try {
      var value = objectMapper.readTree(metadata);
      return value == null ? objectMapper.createObjectNode() : value;
    } catch (JsonProcessingException exception) {
      var legacy = objectMapper.createObjectNode();
      legacy.put("legacy", metadata);
      return legacy;
    }
  }
}
