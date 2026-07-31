package com.testforge.requirement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "requirement_ambiguities")
public class RequirementAmbiguityEntity {
  @Id private UUID id;

  @Column(name = "requirement_id", nullable = false)
  private UUID requirementId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private AmbiguityCategory category;

  @Column(nullable = false, length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AmbiguitySeverity severity;

  @Column(name = "suggested_question", nullable = false, length = 4000)
  private String suggestedQuestion;

  @Column(nullable = false)
  private boolean resolved;

  @Column(length = 4000)
  private String resolution;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Version private long version;

  /** Creates an empty RequirementAmbiguityEntity instance for the persistence framework. */
  protected RequirementAmbiguityEntity() {}

  /** Initializes RequirementAmbiguityEntity with its required collaborators and domain state. */
  private RequirementAmbiguityEntity(
      UUID id,
      UUID requirementId,
      AmbiguityCategory category,
      String description,
      AmbiguitySeverity severity,
      String suggestedQuestion,
      Instant now) {
    this.id = id;
    this.requirementId = requirementId;
    this.category = category;
    this.description = description;
    this.severity = severity;
    this.suggestedQuestion = suggestedQuestion;
    this.createdAt = now;
  }

  /** Creates a new RequirementAmbiguityEntity initialized from the supplied domain values. */
  public static RequirementAmbiguityEntity create(
      UUID requirementId,
      AmbiguityCategory category,
      String description,
      AmbiguitySeverity severity,
      String suggestedQuestion,
      Instant now) {
    return new RequirementAmbiguityEntity(
        UUID.randomUUID(), requirementId, category, description, severity, suggestedQuestion, now);
  }

  /** Executes the resolve operation for RequirementAmbiguityEntity. */
  public void resolve(String resolution, Instant now) {
    this.resolved = true;
    this.resolution = resolution;
    this.resolvedAt = now;
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current requirement id value. */
  public UUID getRequirementId() {
    return requirementId;
  }

  /** Returns the current category value. */
  public AmbiguityCategory getCategory() {
    return category;
  }

  /** Returns the current description value. */
  public String getDescription() {
    return description;
  }

  /** Returns the current severity value. */
  public AmbiguitySeverity getSeverity() {
    return severity;
  }

  /** Returns the current suggested question value. */
  public String getSuggestedQuestion() {
    return suggestedQuestion;
  }

  /** Reports whether resolved. */
  public boolean isResolved() {
    return resolved;
  }

  /** Returns the current resolution value. */
  public String getResolution() {
    return resolution;
  }

  /** Returns the current created at value. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Returns the current resolved at value. */
  public Instant getResolvedAt() {
    return resolvedAt;
  }

  /** Returns the current version value. */
  public long getVersion() {
    return version;
  }
}
