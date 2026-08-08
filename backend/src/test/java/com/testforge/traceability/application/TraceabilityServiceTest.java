package com.testforge.traceability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.testforge.generation.application.ActiveGenerationSetResolver;
import com.testforge.generation.application.LegacyGenerationEvidenceReconciler;
import com.testforge.generation.domain.GenerationSnapshotProvenance;
import com.testforge.generation.repository.GenerationCriterionSnapshotRepository;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.testcase.domain.CoverageType;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.traceability.dto.TraceabilityDtos.LinkedTestCase;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityResponse;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityRow;
import com.testforge.traceability.repository.SnapshotTraceabilityLinkRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TraceabilityServiceTest {
  /** Proves supporting and partial evidence cannot inflate DIRECT coverage metrics. */
  @Test
  void keepsDirectPartialAndSupportingCoverageIndependent() {
    UUID requirementId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID runId = UUID.randomUUID();
    TraceabilityService service =
        spy(
            new TraceabilityService(
                mock(RequirementService.class),
                mock(AcceptanceCriterionRepository.class),
                mock(TestCaseRepository.class),
                mock(GenerationCriterionSnapshotRepository.class),
                mock(SnapshotTraceabilityLinkRepository.class),
                mock(ActiveGenerationSetResolver.class),
                mock(LegacyGenerationEvidenceReconciler.class)));
    TraceabilityResponse matrix =
        new TraceabilityResponse(
            requirementId,
            List.of(
                row(1L, CoverageType.SUPPORTING, TestCaseStatus.APPROVED),
                row(2L, CoverageType.PARTIAL, TestCaseStatus.GENERATED)));
    doReturn(matrix).when(service).traceability(ownerId, requirementId, runId);

    var coverage = service.coverage(ownerId, requirementId, runId);

    assertThat(coverage.totalCriteria()).isEqualTo(2);
    assertThat(coverage.coveredCriteria()).isZero();
    assertThat(coverage.approvedCriteria()).isZero();
    assertThat(coverage.partialCriteria()).isEqualTo(1);
    assertThat(coverage.approvedPartialCriteria()).isZero();
    assertThat(coverage.supportingCriteria()).isEqualTo(1);
    assertThat(coverage.approvedSupportingCriteria()).isEqualTo(1);
  }

  /** Builds one immutable criterion row with one linked case. */
  private TraceabilityRow row(long snapshotId, CoverageType type, TestCaseStatus caseStatus) {
    return new TraceabilityRow(
        snapshotId,
        UUID.randomUUID(),
        "AC-" + snapshotId,
        "Synthetic criterion " + snapshotId,
        4L,
        GenerationSnapshotProvenance.EXACT,
        List.of(
            new LinkedTestCase(
                UUID.randomUUID(),
                "TC-" + snapshotId,
                "Synthetic case " + snapshotId,
                caseStatus,
                type,
                BigDecimal.ONE)));
  }
}
