package com.testforge.audit.repository;

import com.testforge.audit.domain.AuditEventEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
  Page<AuditEventEntity> findAllByProjectIdOrderByTimestampDesc(UUID projectId, Pageable pageable);
}
