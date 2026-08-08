package com.testforge.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.domain.AuditEventEntity;
import com.testforge.audit.repository.AuditEventRepository;
import com.testforge.common.correlation.CorrelationIds;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditEventRepository repository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Initializes AuditService with its required collaborators and domain state. */
  public AuditService(AuditEventRepository repository, ObjectMapper objectMapper, Clock clock) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Executes the record operation for AuditService. */
  public void record(
      UUID actorId,
      UUID projectId,
      String entityType,
      UUID entityId,
      String action,
      AuditMetadata safeMetadata) {
    repository.save(
        AuditEventEntity.create(
            actorId,
            projectId,
            entityType,
            entityId,
            action,
            serialize(safeMetadata.values()),
            clock.instant(),
            CorrelationIds.current()));
  }

  /** Executes the serialize operation for AuditService. */
  private String serialize(Object metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize safe audit metadata.", exception);
    }
  }
}
