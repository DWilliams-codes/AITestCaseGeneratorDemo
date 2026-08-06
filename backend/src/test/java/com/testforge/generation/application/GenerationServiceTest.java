package com.testforge.generation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    when(provider.providerName()).thenReturn("provider");
    when(provider.modelName()).thenReturn("model");
    when(provider.adapterVersion()).thenReturn(GenerationContractVersions.OPENAI_ADAPTER);
    when(transactions.claim(
            any(), any(), anyString(), anyBoolean(), anyString(), anyString(), anyString()))
        .thenReturn(claim);
    lenient()
        .when(transactions.fail(any(), any(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              run.fail(
                  invocation.getArgument(1),
                  invocation.getArgument(2),
                  invocation.getArgument(3),
                  Instant.parse("2026-08-05T12:00:01Z"));
              return run;
            });
    when(activeSets.describe(any(), any()))
        .thenReturn(new ActiveGenerationSetResolver.Metadata(null, Map.of()));
    service = new GenerationService(transactions, provider, validator, activeSets, legacyEvidence);
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
}
