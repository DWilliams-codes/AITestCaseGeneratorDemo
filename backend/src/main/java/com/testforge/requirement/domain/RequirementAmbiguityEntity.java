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

  protected RequirementAmbiguityEntity() {}

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

  public void resolve(String resolution, Instant now) {
    this.resolved = true;
    this.resolution = resolution;
    this.resolvedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRequirementId() {
    return requirementId;
  }

  public AmbiguityCategory getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public AmbiguitySeverity getSeverity() {
    return severity;
  }

  public String getSuggestedQuestion() {
    return suggestedQuestion;
  }

  public boolean isResolved() {
    return resolved;
  }

  public String getResolution() {
    return resolution;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public long getVersion() {
    return version;
  }
}
