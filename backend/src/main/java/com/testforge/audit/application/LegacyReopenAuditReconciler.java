package com.testforge.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.audit.domain.AuditEventEntity;
import com.testforge.audit.repository.AuditEventRepository;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCaseRevisionEntity;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.testcase.repository.TestCaseRevisionRepository;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Transfers legacy reopen reasons out of broadly visible audit metadata exactly once. */
@Component
public class LegacyReopenAuditReconciler {
  public static final int MAX_READ_BATCHES = 1;
  private static final int BATCH_SIZE = 100;
  private static final String ENTITY_TYPE = "TEST_CASE";
  private static final String ACTION = "REOPENED";
  private static final String REASON_FRAGMENT = "\"reason\"";

  private final AuditEventRepository events;
  private final TestCaseRepository testCases;
  private final TestCaseRevisionRepository revisions;
  private final ObjectMapper objectMapper;

  /** Initializes the bridge reconciler with persistence-only collaborators. */
  public LegacyReopenAuditReconciler(
      AuditEventRepository events,
      TestCaseRepository testCases,
      TestCaseRevisionRepository revisions,
      ObjectMapper objectMapper) {
    this.events = events;
    this.testCases = testCases;
    this.revisions = revisions;
    this.objectMapper = objectMapper;
  }

  /** Reconciles one global startup batch and returns its size. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int reconcileBatch() {
    return reconcile(
        () ->
            events.findAllByEntityTypeAndActionAndMetadataContainingOrderByTimestampAscIdAsc(
                ENTITY_TYPE, ACTION, REASON_FRAGMENT, PageRequest.of(0, BATCH_SIZE)));
  }

  /** Reconciles one bounded project batch before audit history is exposed. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int reconcileProject(UUID projectId) {
    return reconcile(
        () ->
            events
                .findAllByProjectIdAndEntityTypeAndActionAndMetadataContainingOrderByTimestampAscIdAsc(
                    projectId,
                    ENTITY_TYPE,
                    ACTION,
                    REASON_FRAGMENT,
                    PageRequest.of(0, BATCH_SIZE)));
  }

  /** Reconciles one bounded case batch before controlled revision history is exposed. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int reconcileTestCase(UUID testCaseId) {
    return reconcile(
        () ->
            events
                .findAllByEntityIdAndEntityTypeAndActionAndMetadataContainingOrderByTimestampAscIdAsc(
                    testCaseId,
                    ENTITY_TYPE,
                    ACTION,
                    REASON_FRAGMENT,
                    PageRequest.of(0, BATCH_SIZE)));
  }

  /** Moves each reason to owner-isolated revision evidence before redacting the audit event. */
  private int reconcile(Supplier<Page<AuditEventEntity>> source) {
    Page<AuditEventEntity> batch = source.get();
    int reconciled = 0;
    for (AuditEventEntity event : batch.getContent()) {
      TestCaseEntity testCase = testCases.findByIdForUpdate(event.getEntityId()).orElse(null);
      if (testCase == null) {
        continue;
      }
      if (!revisions.existsBySourceAuditEventId(event.getId())) {
        revisions.saveAndFlush(
            TestCaseRevisionEntity.legacyReopen(
                testCase.getId(),
                revisions.countByTestCaseId(testCase.getId()) + 1,
                legacySnapshot(testCase.getTestCaseKey()),
                event.getActorId() == null ? testCase.getCreatedBy() : event.getActorId(),
                event.getTimestamp(),
                legacyReason(event.getMetadata()),
                event.getId()));
      }
      event.replaceMetadata(
          serialize(AuditMetadata.reconstructedReopen(testCase.getTestCaseKey()).values()));
      reconciled++;
    }
    events.flush();
    return reconciled;
  }

  /** Extracts only the bounded reason value; malformed legacy JSON becomes explicit provenance. */
  private String legacyReason(String metadata) {
    try {
      JsonNode reason = objectMapper.readTree(metadata).path("reason");
      if (reason.isTextual() && !reason.textValue().isBlank()) {
        String value = reason.textValue().strip();
        return value.substring(0, Math.min(value.length(), 4000));
      }
    } catch (JsonProcessingException ignored) {
      // The audit row is still scrubbed; malformed free-form content is not copied elsewhere.
    }
    return "Legacy reopen reason could not be reconstructed.";
  }

  /** Records honest minimal provenance because the pre-reopen case state is unavailable. */
  private String legacySnapshot(String testCaseKey) {
    return serialize(Map.of("legacyReconstructed", true, "testCaseKey", testCaseKey));
  }

  /** Serializes only application-owned bounded structures. */
  private String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize reconciled audit evidence.", exception);
    }
  }
}
