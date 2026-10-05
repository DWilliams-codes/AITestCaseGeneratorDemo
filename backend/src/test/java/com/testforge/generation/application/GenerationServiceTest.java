package com.testforge.generation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.provider.GenerationProviderException;
import com.testforge.generation.provider.RetryableStructuredOutputException;
import com.testforge.generation.provider.TestGenerationProvider;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.validation.GenerationResultValidator;
import com.testforge.generation.validation.GenerationValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerationServiceTest {
  @Mock private GenerationTransactionService transactions;
  @Mock private TestGenerationProvider provider;
  @Mock private GenerationResultValidator validator;
  @Mock private ActiveGenerationSetResolver activeSets;
  @Mock private LegacyGenerationEvidenceReconciler legacyEvidence;
  @Mock private TestGenerationResult result;

  private GenerationService service;
  private GenerationRunEntity run;
  private TestGenerationRequest request;

  /** Rebuilds one pending claim and terminal transaction behavior for each retry scenario. */
  @BeforeEach
  void setUp() {
    UUID userId = UUID.randomUUID();
    UUID requirementId = UUID.randomUUID();
    request =
        new TestGenerationRequest(
            requirementId,
            "Generate a case",
            "As a tester, I want one case.",
            "One direct case is required.",
            "Synthetic data exists.",
            List.of(new CriterionInput("AC-1", "One case is generated.")),
            "correlation-id");
    run =
        GenerationRunEntity.pending(
            requirementId,
            userId,
            "provider",
            "model",
            GenerationContractVersions.PROMPT,
            "input-hash",
            "key-hash",
            "correlation-id",
            Instant.parse("2026-08-05T12:00:00Z"));
    run.recordRelease(
        GenerationContractVersions.OPENAI_ADAPTER,
        GenerationContractVersions.RESULT,
        GenerationContractVersions.SCHEMA,
        GenerationContractVersions.VALIDATOR,
        4L);
    var claim =
        new GenerationTransactionService.Claim(
            run,
            request,
            Map.of(
                "AC-1", new GenerationTransactionService.CapturedCriterion(1L, UUID.randomUUID())),
            false);
    lenient().when(provider.providerName()).thenReturn("provider");
    lenient().when(provider.modelName()).thenReturn("model");
    lenient().when(provider.adapterVersion()).thenReturn(GenerationContractVersions.OPENAI_ADAPTER);
    lenient()
        .when(
            transactions.claim(
                any(), any(), anyString(), anyBoolean(), anyString(), anyString(), anyString()))
        .thenReturn(claim);
    lenient()
        .when(transactions.fail(any(), any(), anyString(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              TestGenerationResult.UsageMetadata usage = invocation.getArgument(4);
              run.recordUsage(usage.inputTokens(), usage.outputTokens());
              run.fail(
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3),
                  Instant.parse("2026-08-05T12:00:01Z"));
              return run;
            });
    lenient()
        .when(activeSets.describe(any(), any()))
        .thenReturn(new ActiveGenerationSetResolver.Metadata(null, Map.of()));
    service =
        new GenerationService(
            transactions,
            provider,
            validator,
            activeSets,
            legacyEvidence,
            org.mockito.Mockito.mock(com.testforge.config.DemoModePolicy.class));
  }

  /** Retries retryable structured output once and succeeds with the second candidate. */
  @Test
  void retriesIncompleteEmptyOrMalformedOutputExactlyOnce() {
    when(provider.generate(request))
        .thenThrow(new RetryableStructuredOutputException("incomplete"))
        .thenReturn(result);
    when(transactions.complete(any(), any()))
        .thenAnswer(
            invocation -> {
              run.complete(1, 2, 3, Instant.parse("2026-08-05T12:00:01Z"));
              return run;
            });

    var response = service.generate(UUID.randomUUID(), request.requirementId(), "retry-key");
    assertThat(response.status()).isEqualTo(GenerationStatus.COMPLETED);
    assertThat(response.resultContractVersion()).isEqualTo(GenerationContractVersions.RESULT);
    verify(provider, times(2)).generate(request);
    verify(validator).validate(request, result);
  }

  /** Retries semantic rejection once and stores validation rejection after exhaustion. */
  @Test
  void retriesSemanticValidationExactlyOnce() {
    when(provider.generate(request)).thenReturn(result);
    doThrow(new GenerationValidationException("unsafe candidate"))
        .when(validator)
        .validate(request, result);

    assertThat(
            service.generate(UUID.randomUUID(), request.requirementId(), "semantic-key").status())
        .isEqualTo(GenerationStatus.REJECTED_BY_VALIDATION);
    verify(provider, times(2)).generate(request);
    verify(validator, times(2)).validate(request, result);
  }

  /** Does not retry refusal, authentication, configuration, or transport failures. */
  @Test
  void doesNotRetryNonStructuredProviderFailures() {
    when(provider.generate(request)).thenThrow(new GenerationProviderException("transport failed"));

    assertThat(service.generate(UUID.randomUUID(), request.requirementId(), "failure-key").status())
        .isEqualTo(GenerationStatus.FAILED);
    verify(provider).generate(request);
  }

  /** Requires explicit confirmation and never reaches the provider for a deletion request. */
  @Test
  void requiresConfirmationBeforeDeletingASupersededSet() {
    assertThatThrownBy(() -> service.deleteSuperseded(UUID.randomUUID(), run.getId(), false))
        .isInstanceOf(com.testforge.common.error.ApiException.class)
        .hasMessageContaining("Confirm deletion");
    verifyNoInteractions(provider);
    verify(transactions, times(0)).getOwned(any(), any());
  }

  /** Delegates a confirmed deletion with the stable set number and no provider call. */
  @Test
  void deletesConfirmedSupersededSetWithoutProviderInvocation() {
    UUID userId = UUID.randomUUID();
    when(transactions.getOwned(userId, run.getId())).thenReturn(run);
    when(activeSets.describe(run.getRequirementId(), List.of(run)))
        .thenReturn(
            new ActiveGenerationSetResolver.Metadata(UUID.randomUUID(), Map.of(run.getId(), 2)));

    service.deleteSuperseded(userId, run.getId(), true);

    verify(transactions).deleteSuperseded(userId, run.getId(), 2);
    verifyNoInteractions(provider);
  }

  /**
   * Counts malformed transport attempts before a valid response, using the same captured source.
   */
  @Test
  void accumulatesMalformedAndValidUsage() {
    when(result.usage()).thenReturn(new TestGenerationResult.UsageMetadata(7, 11));
    when(provider.generate(request))
        .thenThrow(
            new RetryableStructuredOutputException(
                "safe malformed", null, new TestGenerationResult.UsageMetadata(3, 5)))
        .thenReturn(result);
    when(transactions.complete(any(), any()))
        .thenAnswer(
            invocation -> {
              TestGenerationResult saved = invocation.getArgument(1);
              run.complete(
                  1, saved.usage().inputTokens(), saved.usage().outputTokens(), Instant.now());
              return run;
            });
    var response = service.generate(UUID.randomUUID(), request.requirementId(), "usage-success");
    assertThat(response.inputTokens()).isEqualTo(10);
    assertThat(response.outputTokens()).isEqualTo(16);
    verify(provider, times(2)).generate(request);
  }

  /** Stores both rejected semantic candidates' usage despite persisting no cases. */
  @Test
  void accumulatesSemanticRejections() {
    when(result.usage()).thenReturn(new TestGenerationResult.UsageMetadata(7, 11));
    when(provider.generate(request)).thenReturn(result);
    doThrow(new GenerationValidationException("invalid")).when(validator).validate(request, result);
    var response = service.generate(UUID.randomUUID(), request.requirementId(), "usage-rejected");
    assertThat(response.status()).isEqualTo(GenerationStatus.REJECTED_BY_VALIDATION);
    assertThat(response.inputTokens()).isEqualTo(14);
    assertThat(response.outputTokens()).isEqualTo(22);
  }

  /** Includes available usage on refusal and retains unknown dimensions rather than zero. */
  @Test
  void recordsFailureUsageAndUnknownTotals() {
    when(provider.generate(request))
        .thenThrow(
            new GenerationProviderException(
                "refused", null, new TestGenerationResult.UsageMetadata(8, null)));
    var response = service.generate(UUID.randomUUID(), request.requirementId(), "usage-refused");
    assertThat(response.status()).isEqualTo(GenerationStatus.FAILED);
    assertThat(response.inputTokens()).isEqualTo(8);
    assertThat(response.outputTokens()).isNull();
    verify(provider).generate(request);
  }

  /** A missing usage observation on either attempt makes the aggregate unavailable. */
  @Test
  void unknownAttemptNeverBecomesAFabricatedTotal() {
    when(provider.generate(request))
        .thenThrow(new RetryableStructuredOutputException("unknown"))
        .thenThrow(
            new GenerationProviderException(
                "failed", null, new TestGenerationResult.UsageMetadata(7, 11)));
    var response = service.generate(UUID.randomUUID(), request.requirementId(), "usage-unknown");
    assertThat(response.inputTokens()).isNull();
    assertThat(response.outputTokens()).isNull();
    verify(provider, times(2)).generate(request);
  }

  /** Exhausted malformed attempts retain known totals without candidate persistence. */
  @Test
  void accumulatesExhaustedMalformedAttempts() {
    when(provider.generate(request))
        .thenThrow(
            new RetryableStructuredOutputException(
                "safe", null, new TestGenerationResult.UsageMetadata(4, 9)));
    var response = service.generate(UUID.randomUUID(), request.requirementId(), "usage-malformed");
    assertThat(response.status()).isEqualTo(GenerationStatus.FAILED);
    assertThat(response.inputTokens()).isEqualTo(8);
    assertThat(response.outputTokens()).isEqualTo(18);
  }
}
