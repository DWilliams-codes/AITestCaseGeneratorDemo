package com.testforge.audit.application;

import java.util.Map;

/** Closed, bounded audit metadata shapes that exclude user-authored bodies and secrets. */
public final class AuditMetadata {
  private static final AuditMetadata EMPTY = new AuditMetadata(Map.of());

  private final Map<String, Object> values;

  /** Initializes AuditMetadata with its required collaborators and domain state. */
  private AuditMetadata(Map<String, Object> values) {
    this.values = Map.copyOf(values);
  }

  /** Executes the empty operation for AuditMetadata. */
  public static AuditMetadata empty() {
    return EMPTY;
  }

  /** Requires ment status for the current operation. */
  public static AuditMetadata requirementStatus(String status) {
    return new AuditMetadata(Map.of("newStatus", status));
  }

  /** Executes the criterion operation for AuditMetadata. */
  public static AuditMetadata criterion(String criterionKey) {
    return new AuditMetadata(Map.of("criterionKey", criterionKey));
  }

  /** Executes the generated operation for AuditMetadata. */
  public static AuditMetadata generated(int caseCount, String provider) {
    return new AuditMetadata(Map.of("caseCount", caseCount, "provider", provider));
  }

  /** Exports ed for the current operation. */
  public static AuditMetadata exported(String format, int approvedCaseCount) {
    return new AuditMetadata(Map.of("format", format, "approvedCaseCount", approvedCaseCount));
  }

  /** Executes the test case operation for AuditMetadata. */
  public static AuditMetadata testCase(String testCaseKey) {
    return new AuditMetadata(Map.of("testCaseKey", testCaseKey));
  }

  /** Executes the reviewed operation for AuditMetadata. */
  public static AuditMetadata reviewed(String decision, String testCaseKey) {
    return new AuditMetadata(Map.of("decision", decision, "testCaseKey", testCaseKey));
  }

  /** Executes the reopened operation for AuditMetadata. */
  public static AuditMetadata reopened(String testCaseKey) {
    return new AuditMetadata(Map.of("reasonRecorded", true, "testCaseKey", testCaseKey));
  }

  /** Marks that sensitive bridge metadata was transferred to controlled revision evidence. */
  public static AuditMetadata reconstructedReopen(String testCaseKey) {
    return new AuditMetadata(
        Map.of(
            "legacyReasonMigrated", true,
            "reasonRecorded", true,
            "testCaseKey", testCaseKey));
  }

  /** Redacts unreconciled legacy content without claiming that migration has completed. */
  public static AuditMetadata pendingLegacyReopenRedaction() {
    return new AuditMetadata(
        Map.of(
            "legacyReasonRedacted", true,
            "pendingReconciliation", true,
            "testCaseKey", "legacy-unknown"));
  }

  /** Exposes the immutable closed shape to persistence and response-boundary serializers. */
  public Map<String, Object> values() {
    return values;
  }
}
