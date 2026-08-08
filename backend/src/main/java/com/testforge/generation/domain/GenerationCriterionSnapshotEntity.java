package com.testforge.generation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Immutable criterion text and ordering captured for one generation set. */
@Entity
@Table(name = "generation_criterion_snapshots")
public class GenerationCriterionSnapshotEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "generation_run_id", nullable = false)
  private UUID generationRunId;

  @Column(name = "source_acceptance_criterion_id", nullable = false)
  private UUID sourceAcceptanceCriterionId;

  @Column(name = "criterion_key", nullable = false, length = 20)
  private String criterionKey;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "source_requirement_version", nullable = false)
  private long sourceRequirementVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private GenerationSnapshotProvenance provenance;

  /** Creates an empty snapshot for the persistence framework. */
  protected GenerationCriterionSnapshotEntity() {}

  /** Captures exact source criterion evidence before provider work begins. */
  public static GenerationCriterionSnapshotEntity exact(
      UUID generationRunId,
      UUID sourceAcceptanceCriterionId,
      String criterionKey,
      String description,
      int sortOrder,
      long sourceRequirementVersion) {
    GenerationCriterionSnapshotEntity snapshot = new GenerationCriterionSnapshotEntity();
    snapshot.generationRunId = generationRunId;
    snapshot.sourceAcceptanceCriterionId = sourceAcceptanceCriterionId;
    snapshot.criterionKey = criterionKey;
    snapshot.description = description;
    snapshot.sortOrder = sortOrder;
    snapshot.sourceRequirementVersion = sourceRequirementVersion;
    snapshot.provenance = GenerationSnapshotProvenance.EXACT;
    return snapshot;
  }

  /** Captures only currently knowable criterion evidence for a bridge-era generation run. */
  public static GenerationCriterionSnapshotEntity legacy(
      UUID generationRunId,
      UUID sourceAcceptanceCriterionId,
      String criterionKey,
      String description,
      int sortOrder,
      long sourceRequirementVersion) {
    GenerationCriterionSnapshotEntity snapshot =
        exact(
            generationRunId,
            sourceAcceptanceCriterionId,
            criterionKey,
            description,
            sortOrder,
            sourceRequirementVersion);
    snapshot.provenance = GenerationSnapshotProvenance.LEGACY_RECONSTRUCTED;
    return snapshot;
  }

  /** Returns the current id value. */
  public Long getId() {
    return id;
  }

  /** Returns the current generation run id value. */
  public UUID getGenerationRunId() {
    return generationRunId;
  }

  /** Returns the current source acceptance criterion id value. */
  public UUID getSourceAcceptanceCriterionId() {
    return sourceAcceptanceCriterionId;
  }

  /** Returns the current criterion key value. */
  public String getCriterionKey() {
    return criterionKey;
  }

  /** Returns the current description value. */
  public String getDescription() {
    return description;
  }

  /** Returns the current sort order value. */
  public int getSortOrder() {
    return sortOrder;
  }

  /** Returns the current source requirement version value. */
  public long getSourceRequirementVersion() {
    return sourceRequirementVersion;
  }

  /** Returns the current provenance value. */
  public GenerationSnapshotProvenance getProvenance() {
    return provenance;
  }
}
