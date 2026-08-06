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

  /** Creates an empty AuditEventEntity instance for the persistence framework. */
  protected AuditEventEntity() {}

  /** Initializes AuditEventEntity with its required collaborators and domain state. */
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

  /** Creates a new AuditEventEntity initialized from the supplied domain values. */
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

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current actor id value. */
  public UUID getActorId() {
    return actorId;
  }

  /** Returns the current project id value. */
  public UUID getProjectId() {
    return projectId;
  }

  /** Returns the current entity type value. */
  public String getEntityType() {
    return entityType;
  }

  /** Returns the current entity id value. */
  public UUID getEntityId() {
    return entityId;
  }

  /** Returns the current action value. */
  public String getAction() {
    return action;
  }

  /** Returns the current metadata value. */
  public String getMetadata() {
    return metadata;
  }

  /** Replaces legacy free-form metadata with a closed, bounded audit fact. */
  public void replaceMetadata(String safeMetadata) {
    this.metadata = safeMetadata;
  }

  /** Returns the current timestamp value. */
  public Instant getTimestamp() {
    return timestamp;
  }

  /** Returns the current correlation id value. */
  public String getCorrelationId() {
    return correlationId;
  }
}
