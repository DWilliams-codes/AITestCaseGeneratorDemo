package com.testforge.generation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_runs")
public class GenerationRunEntity {
  @Id private UUID id;

  @Column(name = "requirement_id", nullable = false)
  private UUID requirementId;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Column(nullable = false, length = 100)
  private String provider;

  @Column(nullable = false, length = 200)
  private String model;

  @Column(name = "prompt_version", nullable = false, length = 50)
  private String promptVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private GenerationStatus status;

  @Column(name = "input_hash", nullable = false, length = 64)
  private String inputHash;

  @Column(name = "idempotency_key_hash", nullable = false, length = 64)
  private String idempotencyKeyHash;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "latency_ms")
  private Long latencyMs;

  @Column(name = "generated_case_count", nullable = false)
  private int generatedCaseCount;

  @Column(name = "input_tokens")
  private Integer inputTokens;

  @Column(name = "output_tokens")
  private Integer outputTokens;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "failure_message", length = 500)
  private String failureMessage;

  @Column(name = "correlation_id", nullable = false, length = 100)
  private String correlationId;

  /** Creates an empty GenerationRunEntity instance for the persistence framework. */
  protected GenerationRunEntity() {}

  /** Initializes GenerationRunEntity with its required collaborators and domain state. */
  private GenerationRunEntity(
      UUID id,
      UUID requirementId,
      UUID requestedBy,
      String provider,
      String model,
      String promptVersion,
      String inputHash,
      String idempotencyKeyHash,
      String correlationId,
      Instant now) {
    this.id = id;
    this.requirementId = requirementId;
    this.requestedBy = requestedBy;
    this.provider = provider;
    this.model = model;
    this.promptVersion = promptVersion;
    this.status = GenerationStatus.PENDING;
    this.inputHash = inputHash;
    this.idempotencyKeyHash = idempotencyKeyHash;
    this.correlationId = correlationId;
    this.startedAt = now;
  }

  /** Executes the pending operation for GenerationRunEntity. */
  public static GenerationRunEntity pending(
      UUID requirementId,
      UUID requestedBy,
      String provider,
      String model,
      String promptVersion,
      String inputHash,
      String idempotencyKeyHash,
      String correlationId,
      Instant now) {
    return new GenerationRunEntity(
        UUID.randomUUID(),
        requirementId,
        requestedBy,
        provider,
        model,
        promptVersion,
        inputHash,
        idempotencyKeyHash,
        correlationId,
        now);
  }

  /** Executes the complete operation for GenerationRunEntity. */
  public void complete(int caseCount, int inputTokens, int outputTokens, Instant now) {
    this.status = GenerationStatus.COMPLETED;
    this.generatedCaseCount = caseCount;
    this.inputTokens = inputTokens;
    this.outputTokens = outputTokens;
    this.completedAt = now;
    this.latencyMs = Math.max(0, now.toEpochMilli() - startedAt.toEpochMilli());
  }

  /** Executes the fail operation for GenerationRunEntity. */
  public void fail(GenerationStatus failureStatus, String code, String message, Instant now) {
    if (failureStatus != GenerationStatus.FAILED
        && failureStatus != GenerationStatus.REJECTED_BY_VALIDATION) {
      throw new IllegalArgumentException("Invalid generation failure status");
    }
    this.status = failureStatus;
    this.failureCode = code;
    this.failureMessage = message;
    this.completedAt = now;
    this.latencyMs = Math.max(0, now.toEpochMilli() - startedAt.toEpochMilli());
  }

  /** Returns the current id value. */
  public UUID getId() {
    return id;
  }

  /** Returns the current requirement id value. */
  public UUID getRequirementId() {
    return requirementId;
  }

  /** Returns the current requested by value. */
  public UUID getRequestedBy() {
    return requestedBy;
  }

  /** Returns the current provider value. */
  public String getProvider() {
    return provider;
  }

  /** Returns the current model value. */
  public String getModel() {
    return model;
  }

  /** Returns the current prompt version value. */
  public String getPromptVersion() {
    return promptVersion;
  }

  /** Returns the current status value. */
  public GenerationStatus getStatus() {
    return status;
  }

  /** Returns the current started at value. */
  public Instant getStartedAt() {
    return startedAt;
  }

  /** Returns the current completed at value. */
  public Instant getCompletedAt() {
    return completedAt;
  }

  /** Returns the current latency ms value. */
  public Long getLatencyMs() {
    return latencyMs;
  }

  /** Returns the current generated case count value. */
  public int getGeneratedCaseCount() {
    return generatedCaseCount;
  }

  /** Returns the current input tokens value. */
  public Integer getInputTokens() {
    return inputTokens;
  }

  /** Returns the current output tokens value. */
  public Integer getOutputTokens() {
    return outputTokens;
  }

  /** Returns the current failure code value. */
  public String getFailureCode() {
    return failureCode;
  }

  /** Returns the current failure message value. */
  public String getFailureMessage() {
    return failureMessage;
  }

  /** Returns the current correlation id value. */
  public String getCorrelationId() {
    return correlationId;
  }
}
