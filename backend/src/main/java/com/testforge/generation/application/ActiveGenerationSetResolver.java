package com.testforge.generation.application;

import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.repository.GenerationRunRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** The single source of truth for selecting and numbering successful generation sets. */
@Component
public class ActiveGenerationSetResolver {
  private final GenerationRunRepository runs;

  /** Initializes the resolver with the generation-run store. */
  public ActiveGenerationSetResolver(GenerationRunRepository runs) {
    this.runs = runs;
  }

  /** Resolves the latest successful run; failures never supersede it. */
  public Optional<GenerationRunEntity> resolve(UUID requirementId) {
    return runs.findFirstByRequirementIdAndStatusOrderByCompletedAtDescIdDesc(
        requirementId, GenerationStatus.COMPLETED);
  }

  /** Resolves one successful set without loading the complete history. */
  public Optional<GenerationRunEntity> successful(UUID requirementId, UUID runId) {
    return runs.findByIdAndRequirementIdAndStatus(runId, requirementId, GenerationStatus.COMPLETED);
  }

  /** Resolves active identity and set numbers once for a bounded response collection. */
  public Metadata describe(UUID requirementId, List<GenerationRunEntity> responseRuns) {
    UUID activeRunId = resolve(requirementId).map(GenerationRunEntity::getId).orElse(null);
    if (responseRuns.isEmpty()) {
      return new Metadata(activeRunId, Map.of());
    }
    Map<UUID, Integer> setNumbers =
        runs
            .findSetNumbersByRunIds(responseRuns.stream().map(GenerationRunEntity::getId).toList())
            .stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    item -> UUID.fromString(item.getRunId()),
                    item -> Math.toIntExact(item.getSetNumber())));
    return new Metadata(activeRunId, setNumbers);
  }

  /** Immutable active-set metadata for one bounded response collection. */
  public record Metadata(UUID activeRunId, Map<UUID, Integer> setNumbers) {
    /** Copies caller-owned maps at the response boundary. */
    public Metadata {
      setNumbers = Map.copyOf(setNumbers);
    }

    /** Returns the stable set number, or zero for a non-completed run. */
    public int setNumber(UUID runId) {
      return setNumbers.getOrDefault(runId, 0);
    }

    /** Reports whether the run is the current active successful set. */
    public boolean isActive(UUID runId) {
      return runId.equals(activeRunId);
    }
  }
}
