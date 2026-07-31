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
import com.testforge.config.OpenAiProperties;
import com.testforge.generation.provider.TestGenerationRequest.CriterionInput;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.RequirementSummary;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestPriority;
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
  }

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
            jsonPath("$.input")
                .value(org.hamcrest.Matchers.containsString("untrusted requirement data")))
        .andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));

    TestGenerationResult result = provider.generate(request());

    assertThat(result.testCases()).hasSize(1);
    assertThat(result.usage().inputTokens()).isEqualTo(321);
    assertThat(result.usage().outputTokens()).isEqualTo(654);
    assertThat(provider.providerName()).isEqualTo("openai-responses");
    assertThat(provider.modelName()).isEqualTo("gpt-test");
    server.verify();
  }

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

  private void expectResponse(String body) {
    server.reset();
    server
        .expect(requestTo("https://api.openai.test/v1/responses"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

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
