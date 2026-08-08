package com.testforge.generation.application;

import com.testforge.generation.domain.GenerationCriterionSnapshotEntity;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.repository.GenerationCriterionSnapshotRepository;
import com.testforge.generation.repository.GenerationRunRepository;
import com.testforge.requirement.domain.AcceptanceCriterionEntity;
import com.testforge.requirement.domain.RequirementEntity;
import com.testforge.requirement.repository.AcceptanceCriterionRepository;
import com.testforge.requirement.repository.RequirementRepository;
import com.testforge.testcase.domain.TestCaseEntity;
import com.testforge.testcase.repository.TestCaseRepository;
import com.testforge.traceability.domain.SnapshotTraceabilityLinkEntity;
import com.testforge.traceability.domain.TraceabilityLinkEntity;
import com.testforge.traceability.repository.SnapshotTraceabilityLinkRepository;
import com.testforge.traceability.repository.TraceabilityLinkRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reconciles evidence written by a V5-compatible binary after the V6 migration. */
@Component
public class LegacyGenerationEvidenceReconciler {
  private final GenerationRunRepository runs;
  private final RequirementRepository requirements;
  private final AcceptanceCriterionRepository criteria;
  private final GenerationCriterionSnapshotRepository snapshots;
  private final TraceabilityLinkRepository legacyLinks;
  private final SnapshotTraceabilityLinkRepository snapshotLinks;
  private final TestCaseRepository testCases;

  /** Initializes the bounded bridge reconciler with application-owned evidence stores. */
  public LegacyGenerationEvidenceReconciler(
      GenerationRunRepository runs,
      RequirementRepository requirements,
      AcceptanceCriterionRepository criteria,
      GenerationCriterionSnapshotRepository snapshots,
      TraceabilityLinkRepository legacyLinks,
      SnapshotTraceabilityLinkRepository snapshotLinks,
      TestCaseRepository testCases) {
    this.runs = runs;
    this.requirements = requirements;
    this.criteria = criteria;
    this.snapshots = snapshots;
    this.legacyLinks = legacyLinks;
    this.snapshotLinks = snapshotLinks;
    this.testCases = testCases;
  }

  /** Reconstructs only missing bridge evidence and returns refreshed run entities. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Map<UUID, GenerationRunEntity> reconcile(List<GenerationRunEntity> candidates) {
    if (candidates.isEmpty()) {
      return Map.of();
    }
    List<UUID> legacyIds =
        candidates.stream()
            .filter(run -> run.getSourceSnapshotProvenance() == null)
            .map(GenerationRunEntity::getId)
            .distinct()
            .limit(100)
            .toList();
    if (legacyIds.isEmpty()) {
      return candidates.stream()
          .collect(Collectors.toUnmodifiableMap(GenerationRunEntity::getId, Function.identity()));
    }

    List<GenerationRunEntity> locked = runs.findAllByIdInForUpdate(legacyIds);
    Map<UUID, RequirementEntity> requirementById =
        requirements
            .findAllById(
                locked.stream().map(GenerationRunEntity::getRequirementId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(RequirementEntity::getId, Function.identity()));
    Map<UUID, List<AcceptanceCriterionEntity>> criteriaByRequirement =
        criteria
            .findAllByRequirementIdInOrderByRequirementIdAscSortOrderAsc(
                requirementById.keySet().stream().toList())
            .stream()
            .collect(Collectors.groupingBy(AcceptanceCriterionEntity::getRequirementId));
    Map<UUID, List<GenerationCriterionSnapshotEntity>> snapshotsByRun =
        snapshots.findAllByGenerationRunIdIn(legacyIds).stream()
            .collect(Collectors.groupingBy(GenerationCriterionSnapshotEntity::getGenerationRunId));

    for (GenerationRunEntity run : locked) {
      if (run.getSourceSnapshotProvenance() != null) {
        continue;
      }
      RequirementEntity requirement = requirementById.get(run.getRequirementId());
      if (requirement == null) {
        continue;
      }
      run.recordLegacyRelease(requirement.getVersion());
      if (snapshotsByRun.getOrDefault(run.getId(), List.of()).isEmpty()) {
        List<GenerationCriterionSnapshotEntity> reconstructed =
            snapshots.saveAllAndFlush(
                criteriaByRequirement.getOrDefault(run.getRequirementId(), List.of()).stream()
                    .map(
                        criterion ->
                            GenerationCriterionSnapshotEntity.legacy(
                                run.getId(),
                                criterion.getId(),
                                criterion.getCriterionKey(),
                                criterion.getDescription(),
                                criterion.getSortOrder(),
                                requirement.getVersion()))
                    .toList());
        snapshotsByRun.put(run.getId(), reconstructed);
      }
    }

    List<TraceabilityLinkEntity> bridgeLinks = legacyLinks.findAllByGenerationRunIdIn(legacyIds);
    Map<UUID, UUID> runByCase =
        testCases
            .findAllById(
                bridgeLinks.stream().map(TraceabilityLinkEntity::getTestCaseId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(TestCaseEntity::getId, TestCaseEntity::getGenerationRunId));
    Map<UUID, Map<UUID, Long>> snapshotIdByRunAndCriterion = new HashMap<>();
    snapshotsByRun.forEach(
        (runId, runSnapshots) ->
            snapshotIdByRunAndCriterion.put(
                runId,
                runSnapshots.stream()
                    .collect(
                        Collectors.toMap(
                            GenerationCriterionSnapshotEntity::getSourceAcceptanceCriterionId,
                            GenerationCriterionSnapshotEntity::getId))));
    List<Long> snapshotIds =
        snapshotsByRun.values().stream()
            .flatMap(List::stream)
            .map(GenerationCriterionSnapshotEntity::getId)
            .toList();
    Set<SnapshotCaseKey> persistedLinks =
        new HashSet<>(
            snapshotLinks.findAllByCriterionSnapshotIdIn(snapshotIds).stream()
                .map(
                    link ->
                        new SnapshotCaseKey(link.getCriterionSnapshotId(), link.getTestCaseId()))
                .toList());
    List<SnapshotTraceabilityLinkEntity> reconstructedLinks = new ArrayList<>();
    for (TraceabilityLinkEntity legacy : bridgeLinks) {
      UUID runId = runByCase.get(legacy.getTestCaseId());
      if (runId == null) {
        continue;
      }
      Long snapshotId =
          snapshotIdByRunAndCriterion
              .getOrDefault(runId, Map.of())
              .get(legacy.getAcceptanceCriterionId());
      SnapshotCaseKey key = new SnapshotCaseKey(snapshotId, legacy.getTestCaseId());
      if (snapshotId != null && persistedLinks.add(key)) {
        reconstructedLinks.add(
            SnapshotTraceabilityLinkEntity.create(
                snapshotId,
                legacy.getTestCaseId(),
                legacy.getCoverageType(),
                legacy.getConfidence(),
                legacy.getCreatedAt()));
      }
    }
    snapshotLinks.saveAll(reconstructedLinks);
    runs.flush();
    return candidates.stream()
        .map(
            candidate ->
                locked.stream()
                    .filter(run -> run.getId().equals(candidate.getId()))
                    .findFirst()
                    .orElse(candidate))
        .collect(Collectors.toUnmodifiableMap(GenerationRunEntity::getId, Function.identity()));
  }

  private record SnapshotCaseKey(Long snapshotId, UUID testCaseId) {}
}
