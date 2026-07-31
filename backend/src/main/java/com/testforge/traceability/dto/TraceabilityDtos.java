package com.testforge.traceability.dto;

import com.testforge.testcase.domain.CoverageType;
import com.testforge.testcase.domain.TestCaseStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class TraceabilityDtos {
  /** Prevents instantiation because TraceabilityDtos is a static utility namespace. */
  private TraceabilityDtos() {}

  public record LinkedTestCase(
      UUID id,
      String testCaseKey,
      String title,
      TestCaseStatus status,
      CoverageType coverageType,
      BigDecimal confidence) {}

  public record TraceabilityRow(
      UUID acceptanceCriterionId,
      String criterionKey,
      String description,
      List<LinkedTestCase> testCases) {
    /** Initializes TraceabilityRow with its required collaborators and domain state. */
    public TraceabilityRow {
      testCases = testCases == null ? null : List.copyOf(testCases);
    }
  }

  public record TraceabilityResponse(UUID requirementId, List<TraceabilityRow> rows) {
    /** Initializes TraceabilityResponse with its required collaborators and domain state. */
    public TraceabilityResponse {
      rows = rows == null ? null : List.copyOf(rows);
    }
  }

  public record CoverageResponse(
      UUID requirementId,
      int totalCriteria,
      int coveredCriteria,
      int approvedCriteria,
      double coveragePercent,
      double approvedCoveragePercent) {}
}
