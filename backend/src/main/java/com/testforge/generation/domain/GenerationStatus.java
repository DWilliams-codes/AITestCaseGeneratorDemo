package com.testforge.generation.domain;

/** Persisted attempt outcome; only COMPLETED runs can become an active generation set. */
public enum GenerationStatus {
  PENDING,
  COMPLETED,
  FAILED,
  REJECTED_BY_VALIDATION
}
