package com.testforge.generation.application;

import com.testforge.config.DemoModePolicy;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationSetState;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.dto.GenerationRunPageResponse;
import com.testforge.generation.dto.GenerationRunResponse;
import com.testforge.generation.provider.RetryableStructuredOutputException;
import com.testforge.generation.provider.TestGenerationProvider;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.validation.GenerationResultValidator;
import com.testforge.generation.validation.GenerationValidationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Coordinates provider work outside database transactions and delegates atomic state changes. */
@Service
public class GenerationService {
  private final GenerationTransactionService transactions;
  private final DemoModePolicy demoMode;
  private final TestGenerationProvider provider;
  private final GenerationResultValidator validator;
  private final ActiveGenerationSetResolver activeSets;
  private final LegacyGenerationEvidenceReconciler legacyEvidence;

  /** Initializes GenerationService with its required collaborators and domain state. */
  public GenerationService(
      GenerationTransactionService transactions,
      TestGenerationProvider provider,
      GenerationResultValidator validator,
      ActiveGenerationSetResolver activeSets,
      LegacyGenerationEvidenceReconciler legacyEvidence,
      DemoModePolicy demoMode) {
    this.demoMode = demoMode;
    this.transactions = transactions;
    this.provider = provider;
    this.validator = validator;
    this.activeSets = activeSets;
    this.legacyEvidence = legacyEvidence;
  }

  /** Generates structured manual test coverage from the requirement input. */
  public GenerationRunResponse generate(UUID userId, UUID requirementId, String idempotencyKey) {
    return generateInternal(userId, requirementId, idempotencyKey, false);
  }

  /** Executes the regenerate operation for GenerationService. */
  public GenerationRunResponse regenerate(
      UUID userId, UUID requirementId, String idempotencyKey, boolean confirmSupersede) {
    return generateInternal(userId, requirementId, idempotencyKey, confirmSupersede);
  }

  /** Executes the generate internal operation for GenerationService. */
  private GenerationRunResponse generateInternal(
      UUID userId, UUID requirementId, String idempotencyKey, boolean confirmSupersede) {
    demoMode.requireGenerationAllowed(userId);
    String idempotencyHash = hash(idempotencyKey);
    GenerationTransactionService.Claim claim =
        transactions.claim(
            userId,
            requirementId,
            idempotencyHash,
            confirmSupersede,
            provider.providerName(),
            provider.modelName(),
            provider.adapterVersion());
    if (claim.existing()) return toResponse(reconciled(List.of(claim.run())).getFirst());
    UsageAccumulator usage = new UsageAccumulator();
    try {
      TestGenerationResult generated = generateValidated(claim.providerRequest(), usage);
      TestGenerationResult result =
          new TestGenerationResult(
              generated.requirementSummary(),
              generated.ambiguities(),
              generated.testCases(),
              usage.total());
      return toResponse(transactions.complete(claim, result));
    } catch (GenerationValidationException exception) {
      return toResponse(
          transactions.fail(
              claim.run().getId(),
              GenerationStatus.REJECTED_BY_VALIDATION,
              "invalid_provider_output",
              safeMessage(exception),
              usage.total()));
    } catch (RuntimeException exception) {
      return toResponse(
          transactions.fail(
              claim.run().getId(),
              GenerationStatus.FAILED,
              "provider_failure",
              "Test-case generation failed safely.",
              usage.total()));
    }
  }

  /** Returns the owned resource identified by the request. */
  public GenerationRunResponse get(UUID userId, UUID runId) {
    GenerationRunEntity run = transactions.getOwned(userId, runId);
    return toResponse(reconciled(List.of(run)).getFirst());
  }

  /**
   * Deletes confirmed generated content from one eligible superseded set without invoking a
   * provider.
   */
  public void deleteSuperseded(UUID userId, UUID runId, boolean confirmed) {
    if (!confirmed) {
      throw com.testforge.common.error.ApiExceptions.conflict(
          "generation_set_deletion_confirmation_required",
          "Confirm deletion of the superseded generation set.");
    }
    GenerationRunEntity run = transactions.getOwned(userId, runId);
    int setNumber = activeSets.describe(run.getRequirementId(), List.of(run)).setNumber(runId);
    transactions.deleteSuperseded(userId, runId, setNumber);
  }

  /** Lists resources visible to the current owner using the requested page. */
  public List<GenerationRunResponse> list(UUID userId, UUID requirementId) {
    List<GenerationRunEntity> history = reconciled(transactions.listOwned(userId, requirementId));
    var metadata = activeSets.describe(requirementId, history);
    return history.stream().map(run -> toResponse(run, metadata)).toList();
  }

  /** Executes the list page operation for GenerationService. */
  public GenerationRunPageResponse listPage(UUID userId, UUID requirementId, int page, int size) {
    var history = transactions.listOwnedPage(userId, requirementId, PageRequest.of(page, size));
    List<GenerationRunEntity> reconciled = reconciled(history.getContent());
    var metadata = activeSets.describe(requirementId, reconciled);
    return new GenerationRunPageResponse(
        reconciled.stream().map(run -> toResponse(run, metadata)).toList(),
        history.getNumber(),
        history.getSize(),
        history.getTotalElements(),
        history.getTotalPages(),
        history.hasNext(),
        metadata.activeRunId());
  }

  /** Refreshes only bridge-era rows so all generation responses expose a truthful release tuple. */
  private List<GenerationRunEntity> reconciled(List<GenerationRunEntity> history) {
    Map<UUID, GenerationRunEntity> byId = legacyEvidence.reconcile(history);
    return history.stream().map(run -> byId.getOrDefault(run.getId(), run)).toList();
  }

  /** Retries exactly once for semantic or retryable structured-output defects. */
  private TestGenerationResult generateValidated(
      TestGenerationRequest request, UsageAccumulator usage) {
    RuntimeException firstFailure;
    try {
      return invokeAndValidate(request, usage);
    } catch (GenerationValidationException | RetryableStructuredOutputException exception) {
      firstFailure = exception;
    }
    try {
      return invokeAndValidate(request, usage);
    } catch (GenerationValidationException | RetryableStructuredOutputException exception) {
      if (exception != firstFailure) {
        exception.addSuppressed(firstFailure);
      }
      throw exception;
    }
  }

  /** Executes the invoke and validate operation for GenerationService. */
  private TestGenerationResult invokeAndValidate(
      TestGenerationRequest request, UsageAccumulator usage) {
    TestGenerationResult result;
    try {
      result = provider.generate(request);
      usage.add(result == null ? null : result.usage());
    } catch (com.testforge.generation.provider.GenerationProviderException exception) {
      usage.add(exception.usage());
      throw exception;
    } catch (RuntimeException exception) {
      usage.add(null);
      throw exception;
    }
    validator.validate(request, result);
    return result;
  }

  /** Accumulates all attempted calls; one unavailable dimension makes that total unknown. */
  private static final class UsageAccumulator {
    private Integer input = 0;
    private Integer output = 0;

    /** Includes transport usage before semantic validation can reject a candidate. */
    void add(TestGenerationResult.UsageMetadata usage) {
      input = sum(input, usage == null ? null : usage.inputTokens());
      output = sum(output, usage == null ? null : usage.outputTokens());
    }

    /** Returns unknown rather than overflowing or fabricating an incomplete total. */
    private Integer sum(Integer current, Integer next) {
      return current == null || next == null || (long) current + next > Integer.MAX_VALUE
          ? null
          : current + next;
    }

    /** Returns a detached aggregate suitable for terminal persistence. */
    TestGenerationResult.UsageMetadata total() {
      return new TestGenerationResult.UsageMetadata(input, output);
    }
  }

  /** Maps the source data to response. */
  private GenerationRunResponse toResponse(GenerationRunEntity run) {
    return toResponse(run, activeSets.describe(run.getRequirementId(), List.of(run)));
  }

  /** Maps one run using active-set metadata already loaded for its response collection. */
  private GenerationRunResponse toResponse(
      GenerationRunEntity run, ActiveGenerationSetResolver.Metadata metadata) {
    boolean successful = run.getStatus() == GenerationStatus.COMPLETED;
    return new GenerationRunResponse(
        run.getId(),
        run.getRequirementId(),
        run.getProvider(),
        run.getModel(),
        run.getPromptVersion(),
        run.getProviderAdapterVersion(),
        run.getResultContractVersion(),
        run.getSchemaVersion(),
        run.getValidatorVersion(),
        run.getSourceRequirementVersion(),
        run.getSourceSnapshotProvenance(),
        run.getStatus(),
        run.getStartedAt(),
        run.getCompletedAt(),
        run.getLatencyMs(),
        run.getGeneratedCaseCount(),
        run.getInputTokens(),
        run.getOutputTokens(),
        run.getFailureCode(),
        run.getFailureMessage(),
        run.getCorrelationId(),
        metadata.setNumber(run.getId()),
        transactions.isDeletable(run, metadata.isActive(run.getId())),
        successful
            ? (metadata.isActive(run.getId())
                ? GenerationSetState.ACTIVE
                : GenerationSetState.SUPERSEDED)
            : null);
  }

  /** Reports whether the result h. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }

  /** Executes the safe message operation for GenerationService. */
  private String safeMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return "Provider output did not satisfy the TestForge schema.";
    }
    return message.substring(0, Math.min(message.length(), 500));
  }
}
