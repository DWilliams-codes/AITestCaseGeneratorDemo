package com.testforge.testcase.domain;

/** Classifies why a controlled immutable test-case revision was recorded. */
public enum TestCaseRevisionChangeType {
  EDIT,
  REOPEN,
  LEGACY_REOPEN
}
