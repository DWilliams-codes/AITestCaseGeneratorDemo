package com.testforge.testcase.domain;

/**
 * Persisted lifecycle: only GENERATED/IN_REVIEW are reviewable, terminal decisions require a
 * reopen, and leaving NEEDS_REVISION for IN_REVIEW requires an actual edit.
 */
public enum TestCaseStatus {
  GENERATED,
  IN_REVIEW,
  APPROVED,
  REJECTED,
  NEEDS_REVISION
}
