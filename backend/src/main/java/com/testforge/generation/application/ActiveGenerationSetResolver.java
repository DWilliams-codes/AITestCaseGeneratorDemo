package com.testforge.generation.application;

import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.repository.GenerationRunRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** The single source of truth for selecting and numbering successful generation sets. */
@Component
public class ActiveGenerationSetResolver {
  private static final Comparator<GenerationRunEntity> SUCCESSFUL_ORDER =
      Comparator.comparing(GenerationRunEntity::getCompletedAt)
          .thenComparing(GenerationRunEntity::getId);

  private final GenerationRunRepository runs;

  /** Initializes the resolver with the generation-run store. */
  public ActiveGenerationSetResolver(GenerationRunRepository runs) {
    this.runs = runs;
  }

  /** Resolves the latest successful run; failures never supersede it. */
  public Optional<GenerationRunEntity> resolve(UUID requirementId) {
    return successful(requirementId).stream().max(SUCCESSFUL_ORDER);
  }

  /** Returns successful generation sets in stable oldest-to-newest order. */
  public List<GenerationRunEntity> successful(UUID requirementId) {
    return runs.findAllByRequirementIdAndStatus(requirementId, GenerationStatus.COMPLETED).stream()
        .sorted(SUCCESSFUL_ORDER)
        .toList();
  }

  /** Returns the stable one-based successful set number, or zero for an unsuccessful run. */
  public int setNumber(GenerationRunEntity run) {
    List<GenerationRunEntity> successful = successful(run.getRequirementId());
    for (int index = 0; index < successful.size(); index++) {
      if (successful.get(index).getId().equals(run.getId())) {
        return index + 1;
      }
    }
    return 0;
  }
}
