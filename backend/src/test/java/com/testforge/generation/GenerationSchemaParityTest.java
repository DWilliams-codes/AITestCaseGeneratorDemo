package com.testforge.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testforge.config.GenerationProperties;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedAmbiguity;
import com.testforge.generation.provider.TestGenerationResult.GeneratedStep;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestData;
import com.testforge.generation.provider.TestGenerationResult.RequirementSummary;
import com.testforge.generation.validation.GenerationResultValidator;
import com.testforge.generation.validation.GenerationValidationException;
import com.testforge.requirement.domain.AmbiguityCategory;
import com.testforge.requirement.domain.AmbiguitySeverity;
import com.testforge.testcase.domain.CoverageIntent;
import com.testforge.testcase.domain.DataSensitivity;
import com.testforge.testcase.domain.TestCaseCategory;
import com.testforge.testcase.domain.TestPriority;
import com.testforge.testcase.validation.TestDataReferencePolicy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

/** Keeps the provider schema ceilings aligned with application-owned semantic validation. */
class GenerationSchemaParityTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GenerationResultValidator validator =
      new GenerationResultValidator(
          new GenerationProperties("fake", 25, 30, Duration.ofMinutes(4)),
          new TestDataReferencePolicy());

  /** Exercises every scalar ceiling at the exact limit and at one character beyond it. */
  @Test
  void acceptsExactScalarLimitsAndRejectsMaximumPlusOne() throws Exception {
    List<Bound> bounds =
        List.of(
            new Bound("/requirementSummary/actor", 4000),
            new Bound("/requirementSummary/goal", 4000),
            new Bound("/requirementSummary/businessValue", 4000),
            new Bound("/requirementSummary/assumptions/0", 4000),
            new Bound("/ambiguities/0/description", 4000),
            new Bound("/ambiguities/0/suggestedQuestion", 4000),
            new Bound("/testCases/0/title", 300),
            new Bound("/testCases/0/objective", 4000),
            new Bound("/testCases/0/preconditions/0", 4000),
            new Bound("/testCases/0/testData/0/description", 2000),
            new Bound("/testCases/0/testData/0/exampleValue", 1000),
            new Bound("/testCases/0/testData/0/generationStrategy", 100),
            new Bound("/testCases/0/steps/0/action", 4000),
            new Bound("/testCases/0/steps/0/expectedResult", 4000),
            new Bound("/testCases/0/finalExpectedOutcome", 4000),
            new Bound("/testCases/0/rationale", 4000));

    for (Bound bound : bounds) {
      validator.validate(request(), mutate(bound.pointer(), "x".repeat(bound.maximum())));
      assertThatThrownBy(
              () ->
                  validator.validate(
                      request(), mutate(bound.pointer(), "x".repeat(bound.maximum() + 1))))
          .as(bound.pointer())
          .isInstanceOf(GenerationValidationException.class);
    }

    String exactDataName = "n".repeat(GenerationResultValidator.MAX_DATA_NAME);
    validator.validate(request(), withDataName(exactDataName));
    assertThatThrownBy(
            () ->
                validator.validate(
                    request(),
                    withDataName("n".repeat(GenerationResultValidator.MAX_DATA_NAME + 1))))
        .as("test-data name")
        .isInstanceOf(GenerationValidationException.class);

    String exactReference = " ".repeat(400) + exactDataName + " ".repeat(400);
    validator.validate(request(), withDataReference(exactDataName, exactReference));
    assertThat(exactReference).hasSize(GenerationResultValidator.MAX_DATA_REFERENCE);
    assertThatThrownBy(
            () ->
                validator.validate(
                    request(), withDataReference(exactDataName, " " + exactReference)))
        .as("step data reference")
        .isInstanceOf(GenerationValidationException.class);

    String exactCriterionKey = "K".repeat(GenerationResultValidator.MAX_CRITERION_KEY);
    validator.validate(request(exactCriterionKey), withCriterionKey(exactCriterionKey));
    String oversizedCriterionKey = exactCriterionKey + "K";
    assertThatThrownBy(
            () ->
                validator.validate(
                    request(oversizedCriterionKey), withCriterionKey(oversizedCriterionKey)))
        .as("criterion key")
        .isInstanceOf(GenerationValidationException.class);
  }

  /** Exercises every provider collection at its exact limit and first rejected size. */
  @Test
  void acceptsExactCollectionLimitsAndRejectsMaximumPlusOne() throws Exception {
    List<CollectionBound> bounds =
        List.of(
            new CollectionBound("/testCases", GenerationResultValidator.MAX_CASES),
            new CollectionBound("/testCases/0/steps", GenerationResultValidator.MAX_STEPS),
            new CollectionBound(
                "/testCases/0/preconditions", GenerationResultValidator.MAX_COLLECTION),
            new CollectionBound("/testCases/0/testData", GenerationResultValidator.MAX_COLLECTION),
            new CollectionBound("/ambiguities", GenerationResultValidator.MAX_AMBIGUITIES),
            new CollectionBound(
                "/requirementSummary/assumptions", GenerationResultValidator.MAX_COLLECTION));

    for (CollectionBound bound : bounds) {
      validator.validate(request(), resizeCollection(bound.pointer(), bound.maximum()));
      assertThatThrownBy(
              () ->
                  validator.validate(
                      request(), resizeCollection(bound.pointer(), bound.maximum() + 1)))
          .as(bound.pointer())
          .isInstanceOf(GenerationValidationException.class);
    }

    List<String> exactKeys =
        IntStream.rangeClosed(1, GenerationResultValidator.MAX_AMBIGUITIES)
            .mapToObj(index -> "AC-" + index)
            .toList();
    validator.validate(request(exactKeys), withCriterionKeys(exactKeys));
    List<String> oversizedKeys =
        IntStream.rangeClosed(1, GenerationResultValidator.MAX_AMBIGUITIES + 1)
            .mapToObj(index -> "AC-" + index)
            .toList();
    assertThatThrownBy(
            () -> validator.validate(request(oversizedKeys), withCriterionKeys(oversizedKeys)))
        .as("criterion mappings")
        .isInstanceOf(GenerationValidationException.class);
  }

  /** Verifies all schema collection and string ceilings match the Java validator constants. */
  @Test
  void schemaV2CeilingsMatchTheApplicationValidatorMatrix() throws Exception {
    JsonNode schema =
        objectMapper.readTree(
            new DefaultResourceLoader()
                .getResource("classpath:prompts/test-generation-schema-v2.json")
                .getInputStream());
    Map<String, Integer> expected =
        Map.ofEntries(
            Map.entry("/properties/testCases/maxItems", GenerationResultValidator.MAX_CASES),
            Map.entry(
                "/properties/testCases/items/properties/steps/maxItems",
                GenerationResultValidator.MAX_STEPS),
            Map.entry(
                "/properties/testCases/items/properties/steps/items/properties/stepNumber/maximum",
                GenerationResultValidator.MAX_STEPS),
            Map.entry(
                "/properties/ambiguities/maxItems", GenerationResultValidator.MAX_AMBIGUITIES),
            Map.entry(
                "/properties/requirementSummary/properties/assumptions/maxItems",
                GenerationResultValidator.MAX_COLLECTION),
            Map.entry(
                "/properties/testCases/items/properties/preconditions/maxItems",
                GenerationResultValidator.MAX_COLLECTION),
            Map.entry(
                "/properties/testCases/items/properties/testData/maxItems",
                GenerationResultValidator.MAX_COLLECTION),
            Map.entry(
                "/properties/testCases/items/properties/acceptanceCriteriaKeys/maxItems",
                GenerationResultValidator.MAX_AMBIGUITIES),
            Map.entry(
                "/properties/requirementSummary/properties/actor/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/requirementSummary/properties/goal/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/requirementSummary/properties/businessValue/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/requirementSummary/properties/assumptions/items/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/ambiguities/items/properties/description/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/ambiguities/items/properties/suggestedQuestion/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/title/maxLength",
                GenerationResultValidator.MAX_TITLE),
            Map.entry(
                "/properties/testCases/items/properties/objective/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/preconditions/items/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/testData/items/properties/name/maxLength",
                GenerationResultValidator.MAX_DATA_NAME),
            Map.entry(
                "/properties/testCases/items/properties/testData/items/properties/description/maxLength",
                GenerationResultValidator.MAX_DATA_DESCRIPTION),
            Map.entry(
                "/properties/testCases/items/properties/testData/items/properties/exampleValue/maxLength",
                GenerationResultValidator.MAX_DATA_EXAMPLE),
            Map.entry(
                "/properties/testCases/items/properties/testData/items/properties/generationStrategy/maxLength",
                GenerationResultValidator.MAX_DATA_STRATEGY),
            Map.entry(
                "/properties/testCases/items/properties/steps/items/properties/testDataReference/maxLength",
                GenerationResultValidator.MAX_DATA_REFERENCE),
            Map.entry(
                "/properties/testCases/items/properties/steps/items/properties/action/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/steps/items/properties/expectedResult/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/finalExpectedOutcome/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/rationale/maxLength",
                GenerationResultValidator.MAX_TEXT),
            Map.entry(
                "/properties/testCases/items/properties/acceptanceCriteriaKeys/items/maxLength",
                GenerationResultValidator.MAX_CRITERION_KEY));
    expected.forEach(
        (pointer, maximum) ->
            assertThat(schema.at(pointer).intValue()).as(pointer).isEqualTo(maximum));
  }

  /** Keeps ambiguity severity identical across the domain and provider-output schema. */
  @Test
  void schemaV2AmbiguitySeverityMatchesTheApplicationOutputContract() throws Exception {
    JsonNode schema =
        objectMapper.readTree(
            new DefaultResourceLoader()
                .getResource("classpath:prompts/test-generation-schema-v2.json")
                .getInputStream());
    Set<String> schemaSeverities =
        objectMapper.convertValue(
            schema.at("/properties/ambiguities/items/properties/severity/enum"),
            objectMapper.getTypeFactory().constructCollectionType(Set.class, String.class));
    assertThat(schemaSeverities)
        .containsExactlyInAnyOrder(
            java.util.Arrays.stream(AmbiguitySeverity.values())
                .map(Enum::name)
                .toArray(String[]::new));
  }

  /** Rejects each active-content taxonomy representative in every provider-authored text field. */
  @Test
  void rejectsUnsafeOutputAcrossTheCompleteProviderAuthoredFieldMatrix() throws Exception {
    List<String> fields =
        List.of(
            "/requirementSummary/actor",
            "/requirementSummary/goal",
            "/requirementSummary/businessValue",
            "/requirementSummary/assumptions/0",
            "/ambiguities/0/description",
            "/ambiguities/0/suggestedQuestion",
            "/testCases/0/title",
            "/testCases/0/objective",
            "/testCases/0/preconditions/0",
            "/testCases/0/testData/0/name",
            "/testCases/0/testData/0/description",
            "/testCases/0/testData/0/exampleValue",
            "/testCases/0/testData/0/generationStrategy",
            "/testCases/0/steps/0/action",
            "/testCases/0/steps/0/expectedResult",
            "/testCases/0/steps/0/testDataReference",
            "/testCases/0/finalExpectedOutcome",
            "/testCases/0/acceptanceCriteriaKeys/0",
            "/testCases/0/rationale");
    List<String> unsafe =
        List.of(
            "bash -c echo synthetic",
            "rm -rf ./synthetic",
            "DELETE FROM synthetic_records",
            "fetch('https://example.invalid')",
            "file://synthetic/path",
            "```javascript\nfetch('https://example.invalid')\n```",
            "<img src=x onerror=alert(1)>");
    for (String field : fields) {
      for (String payload : unsafe) {
        assertThatThrownBy(() -> validator.validate(request(), mutate(field, payload)))
            .as(field + " -> " + payload)
            .isInstanceOf(GenerationValidationException.class);
      }
    }
  }

  /** Mutates one provider-authored scalar through its serialized contract path. */
  private TestGenerationResult mutate(String pointer, String value) throws Exception {
    JsonNode root = objectMapper.valueToTree(validResult());
    int separator = pointer.lastIndexOf('/');
    JsonNode parent = root.at(pointer.substring(0, separator));
    String field = pointer.substring(separator + 1);
    if (parent instanceof ObjectNode object) object.put(field, value);
    else
      ((ArrayNode) parent)
          .set(Integer.parseInt(field), objectMapper.getNodeFactory().textNode(value));
    return objectMapper.treeToValue(root, TestGenerationResult.class);
  }

  /** Keeps a bounded data name and its required step reference synchronized. */
  private TestGenerationResult withDataName(String name) throws Exception {
    return withDataReference(name, name);
  }

  /** Builds a candidate with one canonical data name and a boundary-sized reference. */
  private TestGenerationResult withDataReference(String name, String reference) throws Exception {
    ObjectNode root = objectMapper.valueToTree(validResult());
    ((ObjectNode) root.at("/testCases/0/testData/0")).put("name", name);
    ((ObjectNode) root.at("/testCases/0/steps/0")).put("testDataReference", reference);
    return objectMapper.treeToValue(root, TestGenerationResult.class);
  }

  /** Synchronizes the source request and direct mapping for criterion-key boundaries. */
  private TestGenerationResult withCriterionKey(String key) throws Exception {
    return mutate("/testCases/0/acceptanceCriteriaKeys/0", key);
  }

  /** Resizes one known provider collection while retaining all cross-field invariants. */
  private TestGenerationResult resizeCollection(String pointer, int size) throws Exception {
    ObjectNode root = objectMapper.valueToTree(validResult());
    ArrayNode collection = (ArrayNode) root.at(pointer);
    JsonNode seed = collection.get(0).deepCopy();
    collection.removeAll();
    for (int index = 0; index < size; index++) {
      JsonNode item = seed.deepCopy();
      switch (pointer) {
        case "/testCases" -> ((ObjectNode) item).put("title", "Create request " + (index + 1));
        case "/testCases/0/steps" -> {
          ((ObjectNode) item).put("stepNumber", index + 1);
          ((ObjectNode) item).put("action", "Submit synthetic request " + (index + 1) + '.');
          ((ObjectNode) item)
              .put("expectedResult", "Synthetic request " + (index + 1) + " is observed.");
        }
        case "/testCases/0/testData" ->
            ((ObjectNode) item).put("name", index == 0 ? "requestId" : "requestId" + index);
        case "/ambiguities" -> {
          ((ObjectNode) item).put("description", "Clarify boundary " + (index + 1) + '.');
          ((ObjectNode) item)
              .put("suggestedQuestion", "Which boundary " + (index + 1) + " applies?");
        }
        case "/testCases/0/preconditions" ->
            item = objectMapper.getNodeFactory().textNode("Synthetic precondition " + (index + 1));
        case "/requirementSummary/assumptions" ->
            item = objectMapper.getNodeFactory().textNode("Synthetic assumption " + (index + 1));
        default -> throw new IllegalArgumentException("Unsupported collection pointer: " + pointer);
      }
      collection.add(item);
    }
    return objectMapper.treeToValue(root, TestGenerationResult.class);
  }

  /** Builds a candidate with the supplied direct criterion mappings. */
  private TestGenerationResult withCriterionKeys(List<String> keys) throws Exception {
    ObjectNode root = objectMapper.valueToTree(validResult());
    ArrayNode mappings = (ArrayNode) root.at("/testCases/0/acceptanceCriteriaKeys");
    mappings.removeAll();
    keys.forEach(mappings::add);
    return objectMapper.treeToValue(root, TestGenerationResult.class);
  }

  /** Returns one valid request with one direct criterion. */
  private TestGenerationRequest request() {
    return request("AC-1");
  }

  /** Returns one valid request with the supplied direct criterion key. */
  private TestGenerationRequest request(String key) {
    return request(List.of(key));
  }

  /** Returns one valid request containing each supplied direct criterion key. */
  private TestGenerationRequest request(List<String> keys) {
    return new TestGenerationRequest(
        UUID.randomUUID(),
        "Create a request",
        "As a tester, I want one request.",
        "One request is created.",
        "Synthetic records exist.",
        keys.stream()
            .map(
                key ->
                    new TestGenerationRequest.CriterionInput(
                        key, "One synthetic request for " + key + " is created."))
            .toList(),
        "schema-parity");
  }

  /** Returns a valid candidate containing every scalar field in the parity matrix. */
  private TestGenerationResult validResult() {
    return new TestGenerationResult(
        new RequirementSummary("tester", "create request", "traceable result", List.of("safe")),
        List.of(
            new GeneratedAmbiguity(
                AmbiguityCategory.OTHER,
                AmbiguitySeverity.LOW,
                "Clarify the boundary.",
                "Which boundary applies?")),
        List.of(
            new GeneratedTestCase(
                "Create one request",
                "Verify one request is created.",
                TestCaseCategory.HAPPY_PATH,
                TestPriority.HIGH,
                TestPriority.HIGH,
                true,
                CoverageIntent.ACCEPTANCE_CRITERIA,
                List.of("A synthetic record exists."),
                List.of(
                    new GeneratedTestData(
                        "requestId",
                        "Synthetic request identifier.",
                        "TF-001",
                        DataSensitivity.PUBLIC,
                        "Generate a unique value.")),
                List.of(
                    new GeneratedStep(
                        1, "Submit the synthetic request.", "One request is stored.", "requestId")),
                "One request remains stored.",
                List.of("AC-1"),
                "Direct evidence for AC-1.")),
        new TestGenerationResult.UsageMetadata(1, 1));
  }

  private record Bound(String pointer, int maximum) {}

  private record CollectionBound(String pointer, int maximum) {}
}
