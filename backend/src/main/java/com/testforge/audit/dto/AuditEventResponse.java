package com.testforge.audit.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    UUID actorId,
    UUID projectId,
    String entityType,
    UUID entityId,
    String action,
    JsonNode metadata,
    Instant timestamp,
    String correlationId) {}
