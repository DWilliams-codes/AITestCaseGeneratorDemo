package com.testforge.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
  @Id private UUID id;

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "entity_type", nullable = false, length = 100)
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "event_timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "correlation_id", nullable = false, length = 100)
  private String correlationId;

  protected AuditEventEntity() {}

  private AuditEventEntity(
      UUID actorId,
      UUID projectId,
      String entityType,
      UUID entityId,
      String action,
      String metadata,
      Instant timestamp,
      String correlationId) {
    this.id = UUID.randomUUID();
    this.actorId = actorId;
    this.projectId = projectId;
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
    this.metadata = metadata;
    this.timestamp = timestamp;
    this.correlationId = correlationId;
  }

  public static AuditEventEntity create(
      UUID actorId,
      UUID projectId,
      String entityType,
      UUID entityId,
      String action,
      String metadata,
      Instant timestamp,
      String correlationId) {
    return new AuditEventEntity(
        actorId, projectId, entityType, entityId, action, metadata, timestamp, correlationId);
  }

  public UUID getId() {
    return id;
  }

  public UUID getActorId() {
    return actorId;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public String getEntityType() {
    return entityType;
  }

  public UUID getEntityId() {
    return entityId;
  }

  public String getAction() {
    return action;
  }

  public String getMetadata() {
    return metadata;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getCorrelationId() {
    return correlationId;
  }
}
