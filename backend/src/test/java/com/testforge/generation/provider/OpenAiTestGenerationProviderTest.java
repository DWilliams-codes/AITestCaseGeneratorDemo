package com.testforge.generation.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testforge.config.GenerationProperties;
import com.testforge.config.OpenAiProperties;
import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.RequirementSummary;
import com.testforge.generation.validation.GenerationResultValidator;
import com.testforge.generation.validation.GenerationValidationException;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestPriority;
import com.testforge.testcase.validation.TestDataReferencePolicy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiTestGenerationProviderTest {
  private ObjectMapper objectMapper;
  private MockRestServiceServer server;
  private OpenAiTestGenerationProvider provider;
  private GenerationResultValidator validator;

  /** Rebuilds isolated fixtures before each test scenario. */
  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
    server = MockRestServiceServer.bindTo(builder).build();
    provider =
        new OpenAiTestGenerationProvider(
            new OpenAiProperties("test-key", "https://api.openai.test/v1", "gpt-test", 1, 2, 4000),
            objectMapper,
            new DefaultResourceLoader(),
            builder.build());
    validator =
        new GenerationResultValidator(
            new GenerationProperties("openai", 10, 5, Duration.ofMinutes(4)),
            new TestDataReferencePolicy());
  }

  /** Covers the sends a stateless strict schema request and parses usage scenario. */
  @Test
  void sendsAStatelessStrictSchemaRequestAndParsesUsage() throws Exception {
    ObjectNode generated = objectMapper.valueToTree(generatedResult());
    generated.remove("usage");
    ObjectNode response = objectMapper.createObjectNode();
    response.put("status", "completed");
    ObjectNode content = response.putArray("output").addObject().putArray("content").addObject();
    content.put("type", "output_text");
    content.put("text", objectMapper.writeValueAsString(generated));
    response.putObject("usage").put("input_tokens", 321).put("output_tokens", 654);

    server
        .expect(requestTo("https://api.openai.test/v1/responses"))
        .andExpect(method(POST))
        .andExpect(header("Content-Type", "application/json"))
        .andExpect(jsonPath("$.model").value("gpt-test"))
        .andExpect(jsonPath("$.store").value(false))
        .andExpect(jsonPath("$.text.format.type").value("json_schema"))
        .andExpect(jsonPath("$.text.format.strict").value(true))
        .andExpect(
            jsonPath("$.instructions")
                .value(org.hamcrest.Matchers.containsString("internally decompose")))
        .andExpect(
            jsonPath("$.instructions")
                .value(org.hamcrest.Matchers.containsString("smallest coherent suite")))
        .andExpect(
            jsonPath("$.input")
                .value(org.hamcrest.Matchers.containsString("untrusted requirement data")))
        .andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));

    TestGenerationResult result = provider.generate(request());

    assertThat(result.testCases()).hasSize(1);
    assertThat(result.usage().inputTokens()).isEqualTo(321);
    assertThat(result.usage().outputTokens()).isEqualTo(654);
    assertThat(provider.providerName()).isEqualTo("openai-responses");
    assertThat(provider.modelName()).isEqualTo("gpt-test");
    assertThat(provider.adapterVersion()).isEqualTo("openai-responses-v3");
    server.verify();
  }

  /** Covers the safely rejects incomplete empty refused and transport failures scenario. */
  @Test
  void safelyRejectsIncompleteEmptyRefusedAndTransportFailures() {
    expectResponse("{\"status\":\"incomplete\",\"output\":[]}");
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(GenerationProviderException.class)
        .hasMessageContaining("incomplete");

    expectResponse("{\"status\":\"completed\",\"output\":[]}");
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(GenerationProviderException.class)
        .hasMessageContaining("no structured output");

    expectResponse(
        "{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"refusal\",\"refusal\":\"declined\"}]}]}");
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(GenerationProviderException.class)
        .hasMessageContaining("declined");

    expectResponse(
        "{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"not-json\"}]}]}");
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(GenerationProviderException.class)
        .hasMessageContaining("malformed");

    server.reset();
    server.expect(requestTo("https://api.openai.test/v1/responses")).andRespond(withServerError());
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(GenerationProviderException.class)
        .hasMessageContaining("could not complete");
    server.verify();
  }

  /** Fails locally when a schema introduces a keyword outside the provider strict subset. */
  @Test
  void preflightsThePinnedStrictSchemaAndRejectsUnsupportedKeywords() throws Exception {
    var schema =
        objectMapper.readTree(
            new DefaultResourceLoader()
                .getResource("classpath:prompts/test-generation-schema-v2.json")
                .getInputStream());
    assertThat(schema.toString()).doesNotContain("uniqueItems");
    OpenAiTestGenerationProvider.validateStrictSchema(schema, "$", false);

    var unsupported =
        objectMapper.readTree(
            "{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"uniqueItems\":true}");
    assertThatThrownBy(
            () -> OpenAiTestGenerationProvider.validateStrictSchema(unsupported, "$", false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("uniqueItems");
  }

  /** Rejects unknown fields and null primitive values at the local JSON trust boundary. */
  @Test
  void rejectsUnknownFieldsAndNullPrimitivesBeforeSemanticValidation() {
    ObjectNode unknownField = structuredOutput();
    ((ObjectNode) unknownField.at("/testCases/0")).put("unexpectedField", "synthetic");
    expectStructuredOutput(unknownField);
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(RetryableStructuredOutputException.class)
        .hasMessageContaining("malformed");

    ObjectNode nullPrimitive = structuredOutput();
    ((ObjectNode) nullPrimitive.at("/testCases/0/steps/0")).putNull("stepNumber");
    expectStructuredOutput(nullPrimitive);
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(RetryableStructuredOutputException.class)
        .hasMessageContaining("malformed");
  }

  /** Rejects provider-authored transport data and every scalar coercion outside schema types. */
  @Test
  void rejectsRootUsageScalarCoercionFractionalIntegersAndNumericEnums() {
    ObjectNode rootUsage = structuredOutput();
    rootUsage.putObject("usage").put("inputTokens", 999);
    assertMalformed(rootUsage);

    ObjectNode stringBoolean = structuredOutput();
    ((ObjectNode) stringBoolean.at("/testCases/0")).put("automationCandidate", "false");
    assertMalformed(stringBoolean);

    ObjectNode stringInteger = structuredOutput();
    ((ObjectNode) stringInteger.at("/testCases/0/steps/0")).put("stepNumber", "1");
    assertMalformed(stringInteger);

    ObjectNode fractionalInteger = structuredOutput();
    ((ObjectNode) fractionalInteger.at("/testCases/0/steps/0")).put("stepNumber", 1.5);
    assertMalformed(fractionalInteger);

    ObjectNode numericEnum = structuredOutput();
    ((ObjectNode) numericEnum.at("/testCases/0")).put("category", 1);
    assertMalformed(numericEnum);
  }

  /** Preserves missing and null automation-candidate evidence so validation fails closed. */
  @Test
  void rejectsMissingOrNullAutomationCandidateDuringApplicationValidation() {
    ObjectNode missing = structuredOutput();
    ((ObjectNode) missing.at("/testCases/0")).remove("automationCandidate");
    expectStructuredOutput(missing);
    TestGenerationResult missingResult = provider.generate(request());
    assertThat(missingResult.testCases().getFirst().automationCandidate()).isNull();
    assertThatThrownBy(() -> validator.validate(request(), missingResult))
        .isInstanceOf(GenerationValidationException.class);

    ObjectNode explicitNull = structuredOutput();
    ((ObjectNode) explicitNull.at("/testCases/0")).putNull("automationCandidate");
    expectStructuredOutput(explicitNull);
    TestGenerationResult nullResult = provider.generate(request());
    assertThat(nullResult.testCases().getFirst().automationCandidate()).isNull();
    assertThatThrownBy(() -> validator.validate(request(), nullResult))
        .isInstanceOf(GenerationValidationException.class);
  }

  /** Executes the expect response operation for OpenAiTestGenerationProviderTest. */
  private void expectResponse(String body) {
    server.reset();
    server
        .expect(requestTo("https://api.openai.test/v1/responses"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  /** Returns one provider-output object without the response-level usage metadata. */
  private ObjectNode structuredOutput() {
    ObjectNode generated = objectMapper.valueToTree(generatedResult());
    generated.remove("usage");
    return generated;
  }

  /** Wraps one structured candidate in a completed synthetic Responses payload. */
  private void expectStructuredOutput(ObjectNode generated) {
    ObjectNode response = objectMapper.createObjectNode();
    response.put("status", "completed");
    ObjectNode content = response.putArray("output").addObject().putArray("content").addObject();
    content.put("type", "output_text");
    try {
      content.put("text", objectMapper.writeValueAsString(generated));
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize the synthetic candidate.", exception);
    }
    response.putObject("usage").put("input_tokens", 1).put("output_tokens", 1);
    expectResponse(response.toString());
  }

  /** Asserts one schema-inexact provider candidate fails at the local adapter boundary. */
  private void assertMalformed(ObjectNode generated) {
    expectStructuredOutput(generated);
    assertThatThrownBy(() -> provider.generate(request()))
        .isInstanceOf(RetryableStructuredOutputException.class)
        .hasMessageContaining("malformed");
  }

  /** Executes the request operation for OpenAiTestGenerationProviderTest. */
  private TestGenerationRequest request() {
    return new TestGenerationRequest(
        UUID.randomUUID(),
        "Submit a return",
        "As a customer, I want to submit a return.",
        "The return must be eligible.",
        "Synthetic orders are available.",
        List.of(new CriterionInput("AC-1", "One return is created.")),
        "4a91eb76-32fb-4c57-a2d5-458cc98e2d7a");
  }

  /** Executes the generated result operation for OpenAiTestGenerationProviderTest. */
  private TestGenerationResult generatedResult() {
    return new TestGenerationResult(
        new RequirementSummary("customer", "Submit a return", "Recover item value", List.of()),
        List.of(),
        List.of(
            new GeneratedTestCase(
                "Create one eligible return",
                "Verify one return is created.",
                TestCaseCategory.HAPPY_PATH,
                TestPriority.HIGH,
                TestPriority.HIGH,
                true,
                CoverageIntent.ACCEPTANCE_CRITERIA,
                List.of("A synthetic eligible order exists."),
                List.of(),
                List.of(new GeneratedStep(1, "Submit the return.", "One return is created.", null)),
                "One return exists.",
                List.of("AC-1"),
                "Direct evidence for AC-1.")),
        new TestGenerationResult.UsageMetadata(0, 0));
  }
}
