package com.testforge.generation.dto;

import com.testforge.generation.domain.GenerationSetState;
import com.testforge.generation.domain.GenerationStatus;
import java.time.Instant;
import java.util.UUID;

public record GenerationRunResponse(
    UUID id,
    UUID requirementId,
    String provider,
    String model,
    String promptVersion,
    GenerationStatus status,
    Instant startedAt,
    Instant completedAt,
    Long latencyMs,
    int generatedCaseCount,
    Integer inputTokens,
    Integer outputTokens,
    String failureCode,
    String failureMessage,
    String correlationId,
    int setNumber,
    GenerationSetState setState) {}
