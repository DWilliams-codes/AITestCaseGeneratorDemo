package com.testforge.traceability.application;

import com.testforge.common.error.ApiExceptions;
import com.testforge.generation.application.ActiveGenerationSetResolver;
import com.testforge.generation.application.LegacyGenerationEvidenceReconciler;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.repository.GenerationCriterionSnapshotRepository;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.testcase.domain.CoverageType;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.traceability.dto.TraceabilityDtos.CoverageResponse;
import com.testforge.traceability.dto.TraceabilityDtos.LinkedTestCase;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityResponse;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityRow;
import com.testforge.traceability.repository.SnapshotTraceabilityLinkRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TraceabilityService {
  private final RequirementService requirementService;
  private final AcceptanceCriterionRepository criteria;
  private final TestCaseRepository testCases;
  private final GenerationCriterionSnapshotRepository snapshots;
  private final SnapshotTraceabilityLinkRepository snapshotLinks;
  private final ActiveGenerationSetResolver activeSets;
  private final LegacyGenerationEvidenceReconciler legacyEvidence;

  /** Initializes TraceabilityService with its required collaborators and domain state. */
  public TraceabilityService(
      RequirementService requirementService,
      AcceptanceCriterionRepository criteria,
      TestCaseRepository testCases,
      GenerationCriterionSnapshotRepository snapshots,
      SnapshotTraceabilityLinkRepository snapshotLinks,
      ActiveGenerationSetResolver activeSets,
      LegacyGenerationEvidenceReconciler legacyEvidence) {
    this.requirementService = requirementService;
    this.criteria = criteria;
    this.testCases = testCases;
    this.snapshots = snapshots;
    this.snapshotLinks = snapshotLinks;
    this.activeSets = activeSets;
    this.legacyEvidence = legacyEvidence;
  }

  /** Executes the traceability operation for TraceabilityService. */
  @Transactional(readOnly = true)
  public TraceabilityResponse traceability(UUID ownerId, UUID requirementId, UUID generationRunId) {
    requirementService.requireOwned(ownerId, requirementId);
    GenerationRunEntity selectedRun = selectReadableRun(requirementId, generationRunId);
    if (selectedRun == null) {
      List<TraceabilityRow> rows =
          criteria.findAllByRequirementIdOrderBySortOrder(requirementId).stream()
              .map(
                  criterion ->
                      new TraceabilityRow(
                          null,
                          criterion.getId(),
                          criterion.getCriterionKey(),
                          criterion.getDescription(),
                          0,
                          null,
                          List.of()))
              .toList();
      return new TraceabilityResponse(requirementId, rows);
    }
    if (selectedRun.getSourceSnapshotProvenance() == null) {
      legacyEvidence.reconcile(List.of(selectedRun));
    }
    UUID selectedRunId = selectedRun.getId();
    var criterionList = snapshots.findAllByGenerationRunIdOrderBySortOrder(selectedRunId);
    Map<UUID, TestCaseEntity> caseById = new HashMap<>();
    testCases
        .findAllByRequirementIdAndGenerationRunIdOrderByWorkItemNumber(
            requirementId, selectedRunId, PageRequest.of(0, 100))
        .getContent()
        .forEach(item -> caseById.put(item.getId(), item));
    var allLinks =
        snapshotLinks.findAllByCriterionSnapshotIdIn(
            criterionList.stream().map(item -> item.getId()).toList());
    List<TraceabilityRow> rows =
        criterionList.stream()
            .map(
                criterion ->
                    new TraceabilityRow(
                        criterion.getId(),
                        criterion.getSourceAcceptanceCriterionId(),
                        criterion.getCriterionKey(),
                        criterion.getDescription(),
                        criterion.getSourceRequirementVersion(),
                        criterion.getProvenance(),
                        allLinks.stream()
                            .filter(link -> link.getCriterionSnapshotId().equals(criterion.getId()))
                            .map(
                                link -> {
                                  TestCaseEntity testCase = caseById.get(link.getTestCaseId());
                                  return testCase == null
                                      ? null
                                      : new LinkedTestCase(
                                          testCase.getId(),
                                          testCase.getTestCaseKey(),
                                          testCase.getTitle(),
                                          testCase.getStatus(),
                                          link.getCoverageType(),
                                          link.getConfidence());
                                })
                            .filter(java.util.Objects::nonNull)
                            .toList()))
            .toList();
    return new TraceabilityResponse(requirementId, rows);
  }

  /** Executes the coverage operation for TraceabilityService. */
  @Transactional(readOnly = true)
  public CoverageResponse coverage(UUID ownerId, UUID requirementId, UUID generationRunId) {
    TraceabilityResponse matrix = traceability(ownerId, requirementId, generationRunId);
    int total = matrix.rows().size();
    int covered = count(matrix, CoverageType.DIRECT, false);
    int approved = count(matrix, CoverageType.DIRECT, true);
    int partial = count(matrix, CoverageType.PARTIAL, false);
    int approvedPartial = count(matrix, CoverageType.PARTIAL, true);
    int supporting = count(matrix, CoverageType.SUPPORTING, false);
    int approvedSupporting = count(matrix, CoverageType.SUPPORTING, true);
    return new CoverageResponse(
        requirementId,
        total,
        covered,
        approved,
        percent(covered, total),
        percent(approved, total),
        partial,
        approvedPartial,
        percent(partial, total),
        percent(approvedPartial, total),
        supporting,
        approvedSupporting,
        percent(supporting, total),
        percent(approvedSupporting, total));
  }

  /** Counts the requested operation matching the supplied criteria. */
  private int count(TraceabilityResponse matrix, CoverageType type, boolean approvedOnly) {
    return Math.toIntExact(
        matrix.rows().stream()
            .filter(
                row ->
                    row.testCases().stream()
                        .anyMatch(
                            item ->
                                item.coverageType() == type
                                    && (!approvedOnly || item.status() == TestCaseStatus.APPROVED)))
            .count());
  }

  /** Selects the active set by default and validates an explicit historical selector. */
  private GenerationRunEntity selectReadableRun(UUID requirementId, UUID requestedRunId) {
    if (requestedRunId == null) {
      return activeSets.resolve(requirementId).orElse(null);
    }
    return activeSets
        .successful(requirementId, requestedRunId)
        .orElseThrow(() -> ApiExceptions.notFound("Successful generation set not found."));
  }

  /** Executes the percent operation for TraceabilityService. */
  private double percent(int numerator, int denominator) {
    if (denominator == 0) {
      return 0;
    }
    return Math.round((numerator * 10000.0) / denominator) / 100.0;
  }
}
