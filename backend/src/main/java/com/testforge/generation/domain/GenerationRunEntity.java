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

  protected GenerationRunEntity() {}

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

  public void complete(int caseCount, int inputTokens, int outputTokens, Instant now) {
    this.status = GenerationStatus.COMPLETED;
    this.generatedCaseCount = caseCount;
    this.inputTokens = inputTokens;
    this.outputTokens = outputTokens;
    this.completedAt = now;
    this.latencyMs = Math.max(0, now.toEpochMilli() - startedAt.toEpochMilli());
  }

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

  public UUID getId() {
    return id;
  }

  public UUID getRequirementId() {
    return requirementId;
  }

  public UUID getRequestedBy() {
    return requestedBy;
  }

  public String getProvider() {
    return provider;
  }

  public String getModel() {
    return model;
  }

  public String getPromptVersion() {
    return promptVersion;
  }

  public GenerationStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Long getLatencyMs() {
    return latencyMs;
  }

  public int getGeneratedCaseCount() {
    return generatedCaseCount;
  }

  public Integer getInputTokens() {
    return inputTokens;
  }

  public Integer getOutputTokens() {
    return outputTokens;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public String getCorrelationId() {
    return correlationId;
  }
}
