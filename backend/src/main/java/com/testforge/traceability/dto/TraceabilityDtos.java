package com.testforge.traceability.dto;

import com.testforge.testcase.domain.CoverageType;
import com.testforge.testcase.domain.TestCaseStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class TraceabilityDtos {
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
    public TraceabilityRow {
      testCases = testCases == null ? null : List.copyOf(testCases);
    }
  }

  public record TraceabilityResponse(UUID requirementId, List<TraceabilityRow> rows) {
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
