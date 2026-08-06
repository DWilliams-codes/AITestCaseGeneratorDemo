package com.testforge.audit.repository;

import com.testforge.audit.domain.AuditEventEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository
    extends JpaRepository<AuditEventEntity, UUID>, JpaSpecificationExecutor<AuditEventEntity> {
  /** Finds all by project id order by timestamp desc for the supplied criteria. */
  Page<AuditEventEntity> findAllByProjectIdOrderByTimestampDesc(UUID projectId, Pageable pageable);

  /** Finds legacy reopen events that still contain a user-authored reason. */
  Page<AuditEventEntity> findAllByEntityTypeAndActionAndMetadataContainingOrderByTimestampAscIdAsc(
      String entityType, String action, String fragment, Pageable pageable);

  /** Finds project-scoped legacy reopen events before returning audit data. */
  Page<AuditEventEntity>
      findAllByProjectIdAndEntityTypeAndActionAndMetadataContainingOrderByTimestampAscIdAsc(
          UUID projectId, String entityType, String action, String fragment, Pageable pageable);

  /** Finds case-scoped legacy reopen events before returning revision data. */
  Page<AuditEventEntity>
      findAllByEntityIdAndEntityTypeAndActionAndMetadataContainingOrderByTimestampAscIdAsc(
          UUID entityId, String entityType, String action, String fragment, Pageable pageable);
}
