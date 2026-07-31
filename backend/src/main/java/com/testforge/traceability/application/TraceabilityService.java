package com.testforge.traceability.application;

import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.domain.TestCaseStatus;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.traceability.dto.TraceabilityDtos.CoverageResponse;
import com.testforge.traceability.dto.TraceabilityDtos.LinkedTestCase;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityResponse;
import com.testforge.traceability.dto.TraceabilityDtos.TraceabilityRow;
import com.testforge.traceability.repository.TraceabilityLinkRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TraceabilityService {
  private final RequirementService requirementService;
  private final AcceptanceCriterionRepository criteria;
  private final TestCaseRepository testCases;
  private final TraceabilityLinkRepository links;

  /** Initializes TraceabilityService with its required collaborators and domain state. */
  public TraceabilityService(
      RequirementService requirementService,
      AcceptanceCriterionRepository criteria,
      TestCaseRepository testCases,
      TraceabilityLinkRepository links) {
    this.requirementService = requirementService;
    this.criteria = criteria;
    this.testCases = testCases;
    this.links = links;
  }

  /** Executes the traceability operation for TraceabilityService. */
  @Transactional(readOnly = true)
  public TraceabilityResponse traceability(UUID ownerId, UUID requirementId) {
    requirementService.requireOwned(ownerId, requirementId);
    var criterionList = criteria.findAllByRequirementIdOrderBySortOrder(requirementId);
    Map<UUID, TestCaseEntity> caseById = new HashMap<>();
    testCases
        .findAllByRequirementIdOrderByWorkItemNumber(requirementId)
        .forEach(item -> caseById.put(item.getId(), item));
    var allLinks =
        links.findAllByAcceptanceCriterionIdIn(
            criterionList.stream().map(item -> item.getId()).toList());
    List<TraceabilityRow> rows =
        criterionList.stream()
            .map(
                criterion ->
                    new TraceabilityRow(
                        criterion.getId(),
                        criterion.getCriterionKey(),
                        criterion.getDescription(),
                        allLinks.stream()
                            .filter(
                                link -> link.getAcceptanceCriterionId().equals(criterion.getId()))
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
  public CoverageResponse coverage(UUID ownerId, UUID requirementId) {
    TraceabilityResponse matrix = traceability(ownerId, requirementId);
    int total = matrix.rows().size();
    int covered =
        Math.toIntExact(matrix.rows().stream().filter(row -> !row.testCases().isEmpty()).count());
    int approved =
        Math.toIntExact(
            matrix.rows().stream()
                .filter(
                    row ->
                        row.testCases().stream()
                            .anyMatch(item -> item.status() == TestCaseStatus.APPROVED))
                .count());
    return new CoverageResponse(
        requirementId, total, covered, approved, percent(covered, total), percent(approved, total));
  }

  /** Executes the percent operation for TraceabilityService. */
  private double percent(int numerator, int denominator) {
    if (denominator == 0) {
      return 0;
    }
    return Math.round((numerator * 10000.0) / denominator) / 100.0;
  }
}
