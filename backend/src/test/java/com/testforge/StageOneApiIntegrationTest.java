package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.testforge.workspace.domain.WorkspaceMembershipEntity;
import com.testforge.workspace.domain.WorkspaceRole;
import com.testforge.workspace.repository.WorkspaceMembershipRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StageOneApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private WorkspaceMembershipRepository workspaceMemberships;

  private String ownerToken;
  private String outsiderToken;
  private String projectId;
  private String ownerWorkspaceId;
  private String requirementId;
  private String testCaseId;

  /** Rebuilds isolated fixtures before each test scenario. */
  @BeforeEach
  void setUpScenario() throws Exception {
    ownerToken = registerOrLogin("owner@testforge.local", "Owner Analyst", "TestForge!Owner2026");
    outsiderToken =
        registerOrLogin("outsider@testforge.local", "Outside Analyst", "TestForge!Outside2026");
    JsonNode ownerWorkspaces =
        json(
            mockMvc
                .perform(
                    get("/api/v1/workspaces").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].callerRole").value("OWNER"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andReturn());
    ownerWorkspaceId = ownerWorkspaces.get(0).get("id").asText();
    JsonNode project =
        json(
            mockMvc
                .perform(
                    post("/api/v1/projects")
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "name",
                                    "Returns Integration Suite",
                                    "description",
                                    "A synthetic project used by the API integration tests."))))
                .andExpect(status().isCreated())
                .andReturn());
    assertThat(project.get("workspaceId").asText()).isEqualTo(ownerWorkspaceId);
    projectId = project.get("id").asText();
    JsonNode requirement =
        json(
            mockMvc
                .perform(
                    post("/api/v1/projects/{projectId}/requirements", projectId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "title",
                                    "Submit an eligible product return",
                                    "userStory",
                                    "As a signed-in customer, I want to submit a return from the order page.",
                                    "businessRequirements",
                                    "The owner may return an item within 30 days. Duplicate requests are rejected and service failures preserve entered values.",
                                    "assumptions",
                                    "A synthetic delivered order exists.",
                                    "sourceReference",
                                    "INT-RET-101",
                                    "acceptanceCriteria",
                                    List.of(
                                        "One return request is created for an eligible order item.",
                                        "A duplicate return request is rejected.")))))
                .andExpect(status().isCreated())
                .andReturn());
    requirementId = requirement.get("id").asText();
    long requirementWorkItemNumber = requirement.get("workItemNumber").asLong();
    assertThat(requirementWorkItemNumber).isGreaterThanOrEqualTo(1000);
    JsonNode run =
        json(
            mockMvc
                .perform(
                    post("/api/v1/requirements/{requirementId}/generate-test-cases", requirementId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", "integration-generation-" + projectId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn());
    assertThat(run.get("generatedCaseCount").asInt()).isGreaterThanOrEqualTo(4);
    JsonNode testCases =
        json(
            mockMvc
                .perform(
                    get("/api/v1/requirements/{requirementId}/test-cases", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    testCaseId = testCases.get(0).get("id").asText();
    List<Long> testCaseWorkItemNumbers =
        testCases.findValues("workItemNumber").stream().map(JsonNode::asLong).toList();
    assertThat(testCaseWorkItemNumbers)
        .doesNotHaveDuplicates()
        .allMatch(number -> number > requirementWorkItemNumber);
    testCases.forEach(
        testCase ->
            assertThat(testCase.get("testCaseKey").asText())
                .isEqualTo("TC-" + testCase.get("workItemNumber").asLong()));
  }

  /** Covers the executes generation review traceability and safe export workflow scenario. */
  @Test
  @Order(1)
  void executesGenerationReviewTraceabilityAndSafeExportWorkflow() throws Exception {
    MvcResult caseResult =
        mockMvc
            .perform(
                get("/api/v1/test-cases/{testCaseId}", testCaseId)
                    .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.steps[0].expectedResult").isNotEmpty())
            .andExpect(jsonPath("$.acceptanceCriteriaKeys[0]").isNotEmpty())
            .andReturn();
    ObjectNode existing = (ObjectNode) json(caseResult);
    ObjectNode update = objectMapper.createObjectNode();
    update.put("title", "=HYPERLINK(\"https://invalid.test\",\"Synthetic\")");
    update.put("objective", existing.get("objective").asText());
    update.put("category", existing.get("category").asText());
    update.put("priority", existing.get("priority").asText());
    update.put("riskLevel", existing.get("riskLevel").asText());
    update.put("automationCandidate", existing.get("automationCandidate").asBoolean());
    update.put("rationale", existing.get("rationale").asText());
    update.put("finalExpectedOutcome", existing.get("finalExpectedOutcome").asText());
    ArrayNode preconditions = update.putArray("preconditions");
    existing
        .get("preconditions")
        .forEach(item -> preconditions.add(item.get("description").asText()));
    update.set("steps", existing.get("steps"));
    update.set("testData", existing.get("testData"));
    update.put("version", existing.get("version").asLong());

    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_REVIEW"));

    mockMvc
        .perform(
            post("/api/v1/test-cases/{testCaseId}/approve", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\"Approved in the integration workflow.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/coverage", requirementId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coveragePercent").value(100.0))
        .andExpect(jsonPath("$.approvedCriteria").value(1));

    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/traceability", requirementId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].testCases[0].coverageType").value("DIRECT"));

    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/export", requirementId)
                .queryParam("format", "csv")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsString())
                    .contains("\"'=HYPERLINK(\"\"https://invalid.test\"\"")
                    .doesNotContain("\"=HYPERLINK"));

    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/export", requirementId)
                .queryParam("format", "json")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
        .andExpect(
            result -> assertThat(result.getResponse().getContentAsString()).contains("APPROVED"));
    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/export", requirementId)
                .queryParam("format", "md")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsString())
                    .contains("# Submit an eligible product return"));
    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/export", requirementId)
                .queryParam("format", "xml")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("unsupported_export_format"));
  }

  /**
   * Covers the prevents cross owner access and rejects missing csrf and unknown fields scenario.
   */
  @Test
  @Order(2)
  void preventsCrossOwnerAccessAndRejectsMissingCsrfAndUnknownFields() throws Exception {
    JsonNode outsiderWorkspaces =
        json(
            mockMvc
                .perform(
                    get("/api/v1/workspaces").header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andReturn());
    UUID outsiderId = UUID.fromString(outsiderWorkspaces.get(0).get("id").asText());
    workspaceMemberships.save(
        WorkspaceMembershipEntity.create(
            UUID.fromString(ownerWorkspaceId),
            outsiderId,
            WorkspaceRole.STAKEHOLDER,
            UUID.fromString(ownerWorkspaceId),
            Instant.now()));
    JsonNode sharedWorkspaceList =
        json(
            mockMvc
                .perform(
                    get("/api/v1/workspaces").header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode sharedWorkspace = findWorkspace(sharedWorkspaceList, ownerWorkspaceId);
    assertThat(sharedWorkspace).isNotNull();
    assertThat(sharedWorkspace.get("callerRole").asText()).isEqualTo("STAKEHOLDER");

    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}", projectId)
                .header("Authorization", bearer(outsiderToken)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}", requirementId)
                .header("Authorization", bearer(outsiderToken)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/test-cases/{testCaseId}", testCaseId)
                .header("Authorization", bearer(outsiderToken)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"csrf@testforge.local\",\"displayName\":\"Blocked Request\",\"password\":\"TestForge!Csrf2026\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/v1/projects")
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Unknown field\",\"description\":\"\",\"ownerId\":\"attacker\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("malformed_request"));
  }

  /** Covers the rotates refresh tokens and detects reuse scenario. */
  @Test
  @Order(3)
  void rotatesRefreshTokensAndDetectsReuse() throws Exception {
    MvcResult registration =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"rotation@testforge.local\",\"displayName\":\"Token Rotation\",\"password\":\"TestForge!Rotation2026\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie first = registration.getResponse().getCookie("testforge_refresh");
    assertThat(first).isNotNull();
    MvcResult refresh =
        mockMvc
            .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(first))
            .andExpect(status().isOk())
            .andReturn();
    Cookie replacement = refresh.getResponse().getCookie("testforge_refresh");
    assertThat(replacement).isNotNull();
    assertThat(replacement.getValue()).isNotEqualTo(first.getValue());
    mockMvc
        .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(first))
        .andExpect(status().isUnauthorized());
  }

  /** Covers the manages requirement criteria ambiguities and optimistic versions scenario. */
  @Test
  @Order(4)
  void managesRequirementCriteriaAmbiguitiesAndOptimisticVersions() throws Exception {
    JsonNode requirement =
        json(
            mockMvc
                .perform(
                    get("/api/v1/requirements/{requirementId}", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/requirements", projectId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].acceptanceCriteriaCount").value(2));

    ObjectNode update = objectMapper.createObjectNode();
    update.put("title", "Submit and track an eligible product return");
    update.put("userStory", requirement.get("userStory").asText());
    update.put("businessRequirements", requirement.get("businessRequirements").asText());
    update.put("assumptions", requirement.get("assumptions").asText());
    update.put("sourceReference", requirement.get("sourceReference").asText());
    update.put("status", "DRAFT");
    update.put("version", requirement.get("version").asLong());
    mockMvc
        .perform(
            patch("/api/v1/requirements/{requirementId}", requirementId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Submit and track an eligible product return"));
    mockMvc
        .perform(
            patch("/api/v1/requirements/{requirementId}", requirementId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("stale_version"));
    update.put("version", requirement.get("version").asLong() + 1);
    update.put("status", "GENERATED");
    mockMvc
        .perform(
            patch("/api/v1/requirements/{requirementId}", requirementId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("invalid_status_transition"));

    JsonNode added =
        json(
            mockMvc
                .perform(
                    post("/api/v1/requirements/{requirementId}/acceptance-criteria", requirementId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"criterionKey\":\"ac-3\",\"description\":\"The status is visible.\",\"sortOrder\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criterionKey").value("AC-3"))
                .andReturn());
    mockMvc
        .perform(
            post("/api/v1/requirements/{requirementId}/acceptance-criteria", requirementId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"criterionKey\":\"AC-3\",\"description\":\"Duplicate.\",\"sortOrder\":3}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("criterion_conflict"));
    mockMvc
        .perform(
            patch("/api/v1/acceptance-criteria/{criterionId}", added.get("id").asText())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"criterionKey\":\"AC-4\",\"description\":\"The status is visible.\",\"sortOrder\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.criterionKey").value("AC-4"));
    mockMvc
        .perform(
            delete("/api/v1/acceptance-criteria/{criterionId}", added.get("id").asText())
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isNoContent());

    JsonNode refreshed =
        json(
            mockMvc
                .perform(
                    get("/api/v1/requirements/{requirementId}", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode ambiguity = refreshed.get("ambiguities").get(0);
    mockMvc
        .perform(
            post("/api/v1/ambiguities/{ambiguityId}/resolve", ambiguity.get("id").asText())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "resolution",
                            "Customers own their return requests.",
                            "version",
                            ambiguity.get("version").asLong()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolved").value(true));
    mockMvc
        .perform(
            post("/api/v1/ambiguities/{ambiguityId}/resolve", ambiguity.get("id").asText())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "resolution",
                            "Stale resolution.",
                            "version",
                            ambiguity.get("version").asLong()))))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            get("/api/v1/requirements/{requirementId}/export", requirementId)
                .queryParam("format", "csv")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("no_approved_test_cases"));
  }

  /** Executes the register or login operation for StageOneApiIntegrationTest. */
  private String registerOrLogin(String email, String displayName, String password)
      throws Exception {
    MvcResult registration =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "email", email, "displayName", displayName, "password", password))))
            .andReturn();
    if (registration.getResponse().getStatus() == 201) {
      return json(registration).get("accessToken").asText();
    }
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
    return json(login).get("accessToken").asText();
  }

  /** Executes the json operation for StageOneApiIntegrationTest. */
  private JsonNode json(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray());
  }

  /** Finds a workspace response by identifier in a workspace-list payload. */
  private JsonNode findWorkspace(JsonNode workspaces, String workspaceId) {
    for (JsonNode workspace : workspaces) {
      if (workspaceId.equals(workspace.get("id").asText())) {
        return workspace;
      }
    }
    return null;
  }

  /** Executes the bearer operation for StageOneApiIntegrationTest. */
  private String bearer(String token) {
    return "Bearer " + token;
  }
}
