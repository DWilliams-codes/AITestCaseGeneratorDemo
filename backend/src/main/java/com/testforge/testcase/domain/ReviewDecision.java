package com.testforge.testcase.domain;

/** Reviewer intent; rejection and change requests require comments before state is changed. */
public enum ReviewDecision {
  APPROVED,
  REJECTED,
  CHANGES_REQUESTED
}
