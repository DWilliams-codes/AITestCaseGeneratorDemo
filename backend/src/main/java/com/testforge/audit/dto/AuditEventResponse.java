package com.testforge.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    UUID actorId,
    UUID projectId,
    String entityType,
    UUID entityId,
    String action,
    String metadata,
    Instant timestamp,
    String correlationId) {}
