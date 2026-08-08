package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.testforge.audit.domain.AuditEventEntity;
import com.testforge.audit.repository.AuditEventRepository;
import com.testforge.generation.application.GenerationService;
import com.testforge.generation.domain.GenerationRunEntity;
import com.testforge.generation.domain.GenerationStatus;
import com.testforge.generation.provider.FakeTestGenerationProvider;
import com.testforge.generation.provider.TestGenerationRequest;
import com.testforge.generation.provider.TestGenerationResult;
import com.testforge.generation.provider.TestGenerationResult.GeneratedTestCase;
import com.testforge.generation.repository.GenerationRunRepository;
import com.testforge.project.application.ProjectService;
import com.testforge.requirement.application.RequirementService;
import com.testforge.testcase.application.TestCaseService;
import com.testforge.user.repository.UserRepository;
import com.testforge.workspace.domain.WorkspaceMembershipEntity;
import com.testforge.workspace.domain.WorkspaceRole;
import com.testforge.workspace.repository.WorkspaceMembershipRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StageOneApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuditEventRepository auditEvents;
  @Autowired private GenerationRunRepository generationRuns;
  @Autowired private UserRepository users;
  @Autowired private WorkspaceMembershipRepository workspaceMemberships;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private ProjectService projectService;
  @Autowired private RequirementService requirementService;
  @Autowired private GenerationService generationService;
  @Autowired private TestCaseService testCaseService;
  @MockitoSpyBean private FakeTestGenerationProvider generationProvider;

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
                .perform(get("/api/v1/workspaces").header("Authorization", bearer(ownerToken)))
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
                .andExpect(jsonPath("$.promptVersion").value("manual-test-v2"))
                .andExpect(jsonPath("$.resultContractVersion").value("manual-test-result-v1"))
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.setState").value("ACTIVE"))
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
                .content("{\"comments\":\"Approved in the integration workflow.\",\"version\":1}"))
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
                .perform(get("/api/v1/workspaces").header("Authorization", bearer(outsiderToken)))
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
                .perform(get("/api/v1/workspaces").header("Authorization", bearer(outsiderToken)))
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

  /** Proves concurrent predecessor reuse revokes the single successful successor. */
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
    UUID rotationUserId = UUID.fromString(json(registration).path("user").path("id").asText());
    Cookie first = registration.getResponse().getCookie("testforge_refresh");
    assertThat(first).isNotNull();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<MvcResult> concurrentRefresh =
        () -> {
          ready.countDown();
          start.await();
          return mockMvc
              .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(first))
              .andReturn();
        };
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<MvcResult> firstAttempt = executor.submit(concurrentRefresh);
      Future<MvcResult> secondAttempt = executor.submit(concurrentRefresh);
      ready.await();
      start.countDown();
      List<MvcResult> results = List.of(firstAttempt.get(), secondAttempt.get());

      assertThat(results.stream().map(result -> result.getResponse().getStatus()).toList())
          .containsExactlyInAnyOrder(200, 401);
      MvcResult success =
          results.stream()
              .filter(result -> result.getResponse().getStatus() == 200)
              .findFirst()
              .orElseThrow();
      Cookie replacement = success.getResponse().getCookie("testforge_refresh");
      assertThat(replacement).isNotNull();
      assertThat(replacement.getValue()).isNotEqualTo(first.getValue());
      List<RefreshSessionRow> sessions = refreshSessions(rotationUserId);
      assertThat(sessions).hasSize(2);
      assertThat(sessions.stream().map(RefreshSessionRow::familyId).distinct()).hasSize(1);
      RefreshSessionRow predecessor =
          sessions.stream()
              .filter(row -> row.replacedByTokenId() != null)
              .findFirst()
              .orElseThrow();
      RefreshSessionRow successor =
          sessions.stream()
              .filter(row -> row.replacedByTokenId() == null)
              .findFirst()
              .orElseThrow();
      assertThat(predecessor.replacedByTokenId()).isEqualTo(successor.id());
      assertThat(sessions).allMatch(row -> row.revokedAt() != null);
      assertThat(predecessor.reuseDetected()).isTrue();
      assertThat(successor.reuseDetected()).isFalse();
      assertThat(auditActionCount(rotationUserId, "TOKEN_REFRESHED")).isEqualTo(1);
      assertThat(auditActionCount(rotationUserId, "TOKEN_REUSE_DETECTED")).isEqualTo(1);

      mockMvc
          .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(replacement))
          .andExpect(status().isUnauthorized());
      assertThat(refreshSessions(rotationUserId)).allMatch(RefreshSessionRow::reuseDetected);
      assertThat(auditActionCount(rotationUserId, "TOKEN_REUSE_DETECTED")).isEqualTo(2);
    } finally {
      executor.shutdownNow();
    }
  }

  /** Reads refresh lineage directly so the H2 integration path proves containment persistence. */
  private List<RefreshSessionRow> refreshSessions(UUID userId) {
    return jdbcTemplate.query(
        "select id, family_id, replaced_by_token_id, revoked_at, reuse_detected from testforge.refresh_token_sessions where user_id = ? order by created_at, id",
        (resultSet, rowNumber) ->
            new RefreshSessionRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("family_id", UUID.class),
                resultSet.getObject("replaced_by_token_id", UUID.class),
                resultSet.getObject("revoked_at", Instant.class),
                resultSet.getBoolean("reuse_detected")),
        userId);
  }

  /** Counts one security audit action for the refresh-token actor. */
  private int auditActionCount(UUID actorId, String action) {
    return jdbcTemplate.queryForObject(
        "select count(*) from testforge.audit_events where actor_id = ? and action = ?",
        Integer.class,
        actorId,
        action);
  }

  private record RefreshSessionRow(
      UUID id, UUID familyId, UUID replacedByTokenId, Instant revokedAt, boolean reuseDetected) {}

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
    JsonNode immutableTraceabilityBefore =
        json(
            mockMvc
                .perform(
                    get("/api/v1/user-stories/{requirementId}/traceability", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].provenance").value("EXACT"))
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

    long currentRequirementVersion = requirement.get("version").asLong() + 1;
    mockMvc
        .perform(
            post("/api/v1/requirements/{requirementId}/acceptance-criteria", requirementId)
                .header("Authorization", bearer(ownerToken))
                .header("If-Match", "\"" + (currentRequirementVersion - 1) + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"criterionKey\":\"AC-99\",\"description\":\"Must not persist.\",\"sortOrder\":2}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("stale_version"));
    JsonNode added =
        json(
            mockMvc
                .perform(
                    post("/api/v1/requirements/{requirementId}/acceptance-criteria", requirementId)
                        .header("Authorization", bearer(ownerToken))
                        .header("If-Match", "\"" + currentRequirementVersion + "\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"criterionKey\":\"ac-3\",\"description\":\"The status is visible.\",\"sortOrder\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criterionKey").value("AC-3"))
                .andReturn());
    JsonNode preChangeRevision =
        objectMapper.readTree(
            jdbcTemplate.queryForObject(
                "select snapshot_json from testforge.requirement_revisions where requirement_id = ? order by revision_number desc limit 1",
                String.class,
                UUID.fromString(requirementId)));
    assertThat(preChangeRevision.get("acceptanceCriteria")).hasSize(2);
    assertThat(preChangeRevision.get("acceptanceCriteria").toString())
        .contains("AC-1", "AC-2")
        .doesNotContain("AC-3");
    mockMvc
        .perform(
            post("/api/v1/requirements/{requirementId}/acceptance-criteria", requirementId)
                .header("Authorization", bearer(ownerToken))
                .header("If-Match", "\"" + (currentRequirementVersion + 1) + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"criterionKey\":\"AC-3\",\"description\":\"Duplicate.\",\"sortOrder\":3}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("criterion_conflict"));
    mockMvc
        .perform(
            patch("/api/v1/acceptance-criteria/{criterionId}", added.get("id").asText())
                .header("Authorization", bearer(ownerToken))
                .header("If-Match", "\"" + (currentRequirementVersion + 1) + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"criterionKey\":\"AC-4\",\"description\":\"The status is visible.\",\"sortOrder\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.criterionKey").value("AC-4"));
    mockMvc
        .perform(
            delete("/api/v1/acceptance-criteria/{criterionId}", added.get("id").asText())
                .header("Authorization", bearer(ownerToken))
                .header("If-Match", "\"" + (currentRequirementVersion + 2) + "\""))
        .andExpect(status().isNoContent());

    JsonNode immutableTraceabilityAfter =
        json(
            mockMvc
                .perform(
                    get("/api/v1/user-stories/{requirementId}/traceability", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(immutableTraceabilityAfter.get("rows"))
        .isEqualTo(immutableTraceabilityBefore.get("rows"));

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

  /** Verifies canonical user-story routes retain legacy DTO and authorization parity. */
  @Test
  @Order(5)
  void exposesCanonicalUserStoryRoutesWithThinLegacyParity() throws Exception {
    JsonNode canonicalStory =
        json(
            mockMvc
                .perform(
                    get("/api/v1/user-stories/{requirementId}", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andReturn());
    JsonNode legacyStory =
        json(
            mockMvc
                .perform(
                    get("/api/v1/requirements/{requirementId}", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(canonicalStory).isEqualTo(legacyStory);

    JsonNode canonicalCases =
        json(
            mockMvc
                .perform(
                    get("/api/v1/user-stories/{requirementId}/test-cases", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode legacyCases =
        json(
            mockMvc
                .perform(
                    get("/api/v1/requirements/{requirementId}/test-cases", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(canonicalCases).isEqualTo(legacyCases);

    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/generation-runs", requirementId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].setNumber").value(1))
        .andExpect(jsonPath("$[0].setState").value("ACTIVE"));
    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}", requirementId)
                .header("Authorization", bearer(outsiderToken)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}", projectId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userStoryCount").value(1))
        .andExpect(jsonPath("$.requirementCount").value(1));
  }

  /** Verifies reviewed/revised active sets require confirmation and history stays read-only. */
  @Test
  @Order(6)
  void preservesImmutableGenerationSetsAndRequiresSupersedeConfirmation() throws Exception {
    JsonNode oldRuns =
        json(
            mockMvc
                .perform(
                    get("/api/v1/user-stories/{requirementId}/generation-runs", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    String oldRunId = oldRuns.get(0).get("id").asText();
    JsonNode existing =
        json(
            mockMvc
                .perform(
                    get("/api/v1/test-cases/{testCaseId}", testCaseId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    ObjectNode update = objectMapper.createObjectNode();
    update.put("title", existing.get("title").asText() + " revised");
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
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk());

    UUID ownerId = users.findByEmailNormalized("owner@testforge.local").orElseThrow().getId();
    Instant failedAt = Instant.now().plusSeconds(30);
    GenerationRunEntity failedRun =
        GenerationRunEntity.pending(
            UUID.fromString(requirementId),
            ownerId,
            "requirement-rules",
            "testforge-rules-v2",
            "manual-test-v1",
            "a".repeat(64),
            "b".repeat(64),
            "failed-run-fixture",
            failedAt);
    failedRun.fail(
        GenerationStatus.FAILED,
        "provider_failure",
        "Synthetic provider failure.",
        failedAt.plusMillis(1));
    generationRuns.saveAndFlush(failedRun);

    JsonNode runHistory =
        json(
            mockMvc
                .perform(
                    get("/api/v1/user-stories/{requirementId}/generation-runs", requirementId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode failedResponse = null;
    JsonNode activeResponse = null;
    for (JsonNode candidate : runHistory) {
      if (failedRun.getId().toString().equals(candidate.get("id").asText())) {
        failedResponse = candidate;
      }
      if (oldRunId.equals(candidate.get("id").asText())) {
        activeResponse = candidate;
      }
    }
    assertThat(failedResponse).isNotNull();
    assertThat(failedResponse.get("status").asText()).isEqualTo("FAILED");
    assertThat(failedResponse.get("setNumber").asInt()).isZero();
    assertThat(
            failedResponse.path("setState").isMissingNode()
                || failedResponse.path("setState").isNull())
        .isTrue();
    assertThat(activeResponse).isNotNull();
    assertThat(activeResponse.get("setState").asText()).isEqualTo("ACTIVE");

    mockMvc
        .perform(
            post("/api/v1/requirements/{requirementId}/generate-test-cases", requirementId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .header("Idempotency-Key", "integration-generation-" + projectId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(oldRunId))
        .andExpect(jsonPath("$.setState").value("ACTIVE"));

    assertSupersessionConfirmationRequired(
        "/api/v1/user-stories/{requirementId}/generate-test-cases",
        "guarded-generate-canonical-" + projectId);
    assertSupersessionConfirmationRequired(
        "/api/v1/requirements/{requirementId}/generate-test-cases",
        "guarded-generate-legacy-" + projectId);
    assertSupersessionConfirmationRequired(
        "/api/v1/user-stories/{requirementId}/regenerate",
        "guarded-regeneration-canonical-" + projectId);
    assertSupersessionConfirmationRequired(
        "/api/v1/requirements/{requirementId}/regenerate",
        "guarded-regeneration-legacy-" + projectId);

    JsonNode newRun =
        json(
            mockMvc
                .perform(
                    post("/api/v1/user-stories/{requirementId}/regenerate", requirementId)
                        .with(csrf())
                        .queryParam("confirmSupersede", "true")
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", "confirmed-regeneration-" + projectId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setNumber").value(2))
                .andExpect(jsonPath("$.setState").value("ACTIVE"))
                .andReturn());

    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/generation-runs/page", requirementId)
                .param("page", "0")
                .param("size", "1")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(failedRun.getId().toString()))
        .andExpect(jsonPath("$.activeGenerationRunId").value(newRun.get("id").asText()));

    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/test-cases", requirementId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].generationRunId").value(newRun.get("id").asText()));
    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/test-cases", requirementId)
                .queryParam("generationRunId", oldRunId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].generationRunId").value(oldRunId));
    update.put("version", 1);
    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("superseded_generation_set"));
  }

  /** Verifies bounded audit filters and inert normalization of legacy metadata. */
  @Test
  @Order(7)
  void filtersAndNormalizesOwnedAuditHistory() throws Exception {
    Instant now = Instant.now();
    UUID projectUuid = UUID.fromString(projectId);
    auditEvents.saveAllAndFlush(
        List.of(
            AuditEventEntity.create(
                null,
                projectUuid,
                "LEGACY",
                projectUuid,
                "INVALID_JSON",
                "not-json",
                now.minusSeconds(1),
                "audit-test-invalid"),
            AuditEventEntity.create(
                null,
                projectUuid,
                "LEGACY",
                projectUuid,
                "NULL_JSON",
                "null",
                now,
                "audit-test-null")));

    JsonNode projectAudit =
        json(
            mockMvc
                .perform(
                    get("/api/v1/projects/{projectId}/audit-events", projectId)
                        .queryParam("entityType", " PROJECT ")
                        .queryParam("entityId", projectId)
                        .queryParam("action", " CREATED ")
                        .queryParam("from", now.minusSeconds(86_400).toString())
                        .queryParam("to", now.plusSeconds(86_400).toString())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].entityType").value("PROJECT"))
                .andExpect(jsonPath("$.items[0].metadata").isMap())
                .andReturn());
    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .queryParam("entityType", "PROJECT")
                .queryParam("entityId", projectId)
                .queryParam("actorId", projectAudit.get("items").get(0).get("actorId").asText())
                .queryParam("action", "CREATED")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .queryParam("entityType", "LEGACY")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].metadata").doesNotExist())
        .andExpect(jsonPath("$.items[1].metadata.legacy").value("not-json"));

    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .queryParam("entityType", " ")
                .queryParam("action", " ")
                .queryParam("from", now.minusSeconds(60).toString())
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .queryParam("to", now.plusSeconds(60).toString())
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .queryParam("from", now.plusSeconds(60).toString())
                .queryParam("to", now.minusSeconds(60).toString())
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("invalid_audit_range"));
    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .queryParam("from", now.minusSeconds(367L * 86_400L).toString())
                .queryParam("to", now.toString())
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("audit_range_too_large"));
  }

  /** Verifies guarded review transitions, canonical data references, and revision history. */
  @Test
  @Order(8)
  void enforcesActiveCaseWorkflowAndRevisionRules() throws Exception {
    JsonNode generated =
        json(
            mockMvc
                .perform(
                    get("/api/v1/test-cases/{testCaseId}", testCaseId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn());

    mockMvc
        .perform(
            post("/api/v1/test-cases/{testCaseId}/request-changes", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\" \",\"version\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("invalid_test_case_transition"));
    JsonNode needsRevision =
        json(
            mockMvc
                .perform(
                    post("/api/v1/test-cases/{testCaseId}/request-changes", testCaseId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comments\":\"Clarify the expected result.\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_REVISION"))
                .andReturn());

    ObjectNode unchanged = editableUpdate(needsRevision);
    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unchanged)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(needsRevision.get("version").asLong()));

    ObjectNode stale = unchanged.deepCopy();
    stale.put("version", generated.get("version").asLong());
    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stale)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("stale_version"));

    ObjectNode invalidOrder = unchanged.deepCopy();
    ((ObjectNode) invalidOrder.withArray("steps").get(0)).put("stepNumber", 2);
    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidOrder)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("invalid_step_order"));

    ObjectNode danglingReference = unchanged.deepCopy();
    ((ObjectNode) danglingReference.withArray("steps").get(0))
        .put("testDataReference", "missing-data-item");
    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(danglingReference)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("invalid_test_data_reference"));

    ObjectNode corrected = unchanged.deepCopy();
    corrected.put("title", corrected.get("title").asText() + " clarified");
    JsonNode firstData = corrected.withArray("testData").get(0);
    if (firstData != null) {
      String canonicalName = firstData.get("name").asText();
      ((ObjectNode) firstData).put("name", " " + canonicalName + " ");
      corrected
          .withArray("steps")
          .forEach(
              step -> {
                if (step.get("testDataReference") != null
                    && !step.get("testDataReference").isNull()) {
                  ((ObjectNode) step)
                      .put("testDataReference", canonicalName.toUpperCase(java.util.Locale.ROOT));
                }
              });
      ObjectNode secondaryData = ((ObjectNode) firstData.deepCopy());
      secondaryData.put("name", "secondarySyntheticData");
      secondaryData.put("exampleValue", "synthetic-secondary-value");
      corrected.withArray("testData").add(secondaryData);
    }
    JsonNode inReview =
        json(
            mockMvc
                .perform(
                    patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corrected)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andReturn());

    ObjectNode reorderedOnly = editableUpdate(inReview);
    ArrayNode reorderedData = reorderedOnly.withArray("testData");
    if (reorderedData.size() > 1) {
      JsonNode firstItem = reorderedData.get(0).deepCopy();
      JsonNode secondItem = reorderedData.get(1).deepCopy();
      reorderedData.removeAll();
      reorderedData.add(secondItem);
      reorderedData.add(firstItem);
    }
    mockMvc
        .perform(
            patch("/api/v1/test-cases/{testCaseId}", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reorderedOnly)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(inReview.get("version").asLong()));

    mockMvc
        .perform(
            get("/api/v1/test-cases/{testCaseId}/revisions", testCaseId)
                .queryParam("size", "1")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].revisionNumber").value(1))
        .andExpect(jsonPath("$.items[0].snapshot.schemaVersion").value(1));

    long reviewVersion = inReview.get("version").asLong();
    mockMvc
        .perform(
            post("/api/v1/test-cases/{testCaseId}/approve", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("comments", "Stale approval", "version", reviewVersion - 1))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("stale_version"));
    JsonNode approved =
        json(
            mockMvc
                .perform(
                    post("/api/v1/test-cases/{testCaseId}/approve", testCaseId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of("comments", "Ready", "version", reviewVersion))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn());
    JsonNode reopened =
        json(
            mockMvc
                .perform(
                    post("/api/v1/test-cases/{testCaseId}/reopen", testCaseId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "reason",
                                    "New evidence",
                                    "version",
                                    approved.get("version").asLong()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andReturn());
    mockMvc
        .perform(
            get("/api/v1/test-cases/{testCaseId}/revisions", testCaseId)
                .queryParam("size", "1")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].changeType").value("REOPEN"))
        .andExpect(jsonPath("$.items[0].changeReason").value("New evidence"));
    AuditEventEntity reopenAudit =
        auditEvents.findAll().stream()
            .filter(event -> "REOPENED".equals(event.getAction()))
            .filter(event -> UUID.fromString(testCaseId).equals(event.getEntityId()))
            .findFirst()
            .orElseThrow();
    assertThat(reopenAudit.getMetadata())
        .contains("\"reasonRecorded\":true")
        .doesNotContain("New evidence");
    mockMvc
        .perform(
            post("/api/v1/test-cases/{testCaseId}/reject", testCaseId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "comments",
                            "Evidence is insufficient",
                            "version",
                            reopened.get("version").asLong()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
  }

  /** Proves provider latency is transaction-free and same-key claims invoke it only once. */
  @Test
  @Order(9)
  void keepsProviderWorkOutsideTransactionsAndDeduplicatesConcurrentClaims() throws Exception {
    reset(generationProvider);
    CountDownLatch providerEntered = new CountDownLatch(1);
    CountDownLatch releaseProvider = new CountDownLatch(1);
    AtomicBoolean providerTransactionActive = new AtomicBoolean(true);
    doAnswer(
            invocation -> {
              providerTransactionActive.set(
                  TransactionSynchronizationManager.isActualTransactionActive());
              providerEntered.countDown();
              if (!releaseProvider.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Provider latch timed out.");
              }
              return invocation.callRealMethod();
            })
        .when(generationProvider)
        .generate(any());

    String idempotencyKey = "concurrent-generation-key";
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<MvcResult> first =
          executor.submit(
              () ->
                  mockMvc
                      .perform(
                          post(
                                  "/api/v1/user-stories/{requirementId}/generate-test-cases",
                                  requirementId)
                              .with(csrf())
                              .header("Authorization", bearer(ownerToken))
                              .header("Idempotency-Key", idempotencyKey))
                      .andReturn());
      assertThat(providerEntered.await(10, TimeUnit.SECONDS)).isTrue();
      assertThat(providerTransactionActive.get()).isFalse();

      mockMvc
          .perform(
              get("/api/v1/projects/{projectId}", projectId)
                  .header("Authorization", bearer(ownerToken)))
          .andExpect(status().isOk());
      mockMvc
          .perform(
              post("/api/v1/user-stories/{requirementId}/generate-test-cases", requirementId)
                  .with(csrf())
                  .header("Authorization", bearer(ownerToken))
                  .header("Idempotency-Key", idempotencyKey))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.status").value("PENDING"));
      verify(generationProvider, times(1)).generate(any());

      releaseProvider.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(201);
      verify(generationProvider, times(1)).generate(any());
    } finally {
      releaseProvider.countDown();
      executor.shutdownNow();
      reset(generationProvider);
    }
  }

  /** Proves collection SQL counts remain constant when page size grows from one to twenty. */
  @Test
  @Order(10)
  void keepsCollectionQueryCountsIndependentOfPageSize() throws Exception {
    UUID ownerId = users.findByEmailNormalized("owner@testforge.local").orElseThrow().getId();
    UUID projectUuid = UUID.fromString(projectId);
    UUID requirementUuid = UUID.fromString(requirementId);
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);

    assertConstantStatementCount(
        statistics,
        () -> projectService.list(ownerId, 0, 1),
        () -> projectService.list(ownerId, 0, 20));
    assertConstantStatementCount(
        statistics,
        () -> requirementService.list(ownerId, projectUuid, 0, 1),
        () -> requirementService.list(ownerId, projectUuid, 0, 20));
    assertConstantStatementCount(
        statistics,
        () -> generationService.listPage(ownerId, requirementUuid, 0, 1),
        () -> generationService.listPage(ownerId, requirementUuid, 0, 20));
    assertConstantStatementCount(
        statistics,
        () -> testCaseService.listPage(ownerId, requirementUuid, null, 0, 1),
        () -> testCaseService.listPage(ownerId, requirementUuid, null, 0, 20));

    UUID activeRunId =
        jdbcTemplate.queryForObject(
            "select generation_run_id from testforge.test_cases where id = ?",
            UUID.class,
            UUID.fromString(testCaseId));
    jdbcTemplate.update(
        "update testforge.test_cases set status = 'GENERATED' where generation_run_id = ?",
        activeRunId);
    jdbcTemplate.update(
        "update testforge.test_cases set status = 'APPROVED' where id = ?",
        UUID.fromString(testCaseId));

    statistics.clear();
    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/export", requirementId)
                .param("generationRunId", activeRunId.toString())
                .param("format", "json")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk());
    long oneCaseExportCount = statistics.getPrepareStatementCount();

    jdbcTemplate.update(
        "update testforge.test_cases set status = 'APPROVED' where generation_run_id = ?",
        activeRunId);
    int existingCases =
        jdbcTemplate.queryForObject(
            "select count(*) from testforge.test_cases where generation_run_id = ?",
            Integer.class,
            activeRunId);
    Instant createdAt = Instant.parse("2026-08-05T19:00:00Z");
    for (int index = existingCases; index < 20; index++) {
      UUID syntheticCaseId = UUID.randomUUID();
      jdbcTemplate.update(
          "insert into testforge.test_cases (id, requirement_id, generation_run_id, test_case_key, title, objective, category, priority, risk_level, automation_candidate, status, coverage_intent, rationale, final_expected_outcome, created_by, created_at, updated_at, version) values (?, ?, ?, ?, 'Export query case', 'Verify bounded export assembly.', 'HAPPY_PATH', 'MEDIUM', 'MEDIUM', false, 'APPROVED', 'ACCEPTANCE_CRITERIA', 'Synthetic query-count evidence.', 'Export remains bounded.', ?, ?, ?, 0)",
          syntheticCaseId,
          requirementUuid,
          activeRunId,
          "TC-Q-" + syntheticCaseId.toString().substring(0, 8),
          ownerId,
          createdAt.plusSeconds(index),
          createdAt.plusSeconds(index));
    }

    statistics.clear();
    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/export", requirementId)
                .param("generationRunId", activeRunId.toString())
                .param("format", "json")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk());
    long twentyCaseExportCount = statistics.getPrepareStatementCount();

    assertThat(twentyCaseExportCount)
        .isLessThanOrEqualTo(oneCaseExportCount)
        .isLessThanOrEqualTo(12);
  }

  /** Reconciles a V5-shaped post-migration write before direct export reads snapshot evidence. */
  @Test
  @Order(11)
  void reconcilesGenerationEvidenceWrittenByABridgeBinaryBeforeDirectExport() throws Exception {
    UUID ownerId = users.findByEmailNormalized("owner@testforge.local").orElseThrow().getId();
    UUID requirementUuid = UUID.fromString(requirementId);
    UUID runId = UUID.randomUUID();
    UUID caseId = UUID.randomUUID();
    UUID criterionId =
        jdbcTemplate.queryForObject(
            "select id from testforge.acceptance_criteria where requirement_id = ? order by sort_order fetch first 1 row only",
            UUID.class,
            requirementUuid);
    Instant completedAt = Instant.parse("2026-08-05T20:00:00Z");
    jdbcTemplate.update(
        "insert into testforge.generation_runs (id, requirement_id, requested_by, provider, model, prompt_version, status, input_hash, idempotency_key_hash, started_at, completed_at, latency_ms, generated_case_count, input_tokens, output_tokens, correlation_id) values (?, ?, ?, 'legacy-provider', 'legacy-model', 'legacy-prompt', 'COMPLETED', ?, ?, ?, ?, 10, 1, 1, 1, 'legacy-correlation')",
        runId,
        requirementUuid,
        ownerId,
        "a".repeat(64),
        "b".repeat(64),
        completedAt.minusSeconds(1),
        completedAt);
    jdbcTemplate.update(
        "insert into testforge.test_cases (id, requirement_id, generation_run_id, test_case_key, title, objective, category, priority, risk_level, automation_candidate, status, coverage_intent, rationale, final_expected_outcome, created_by, created_at, updated_at, version) values (?, ?, ?, ?, 'Legacy bridge case', 'Verify reconstructed evidence.', 'HAPPY_PATH', 'HIGH', 'HIGH', false, 'APPROVED', 'ACCEPTANCE_CRITERIA', 'Legacy bridge evidence.', 'Evidence remains traceable.', ?, ?, ?, 0)",
        caseId,
        requirementUuid,
        runId,
        "TC-LG-" + runId.toString().substring(0, 8),
        ownerId,
        completedAt,
        completedAt);
    jdbcTemplate.update(
        "insert into testforge.traceability_links (id, acceptance_criterion_id, test_case_id, coverage_type, confidence, created_at) values (?, ?, ?, 'DIRECT', 0.9500, ?)",
        UUID.randomUUID(),
        criterionId,
        caseId,
        completedAt);

    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/export", requirementId)
                .param("generationRunId", runId.toString())
                .param("format", "json")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(caseId.toString()))
        .andExpect(jsonPath("$[0].acceptanceCriteriaKeys[0]").value("AC-1"));
    assertThat(
            jdbcTemplate.queryForObject(
                "select source_snapshot_provenance from testforge.generation_runs where id = ?",
                String.class,
                runId))
        .isEqualTo("LEGACY_RECONSTRUCTED");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.snapshot_traceability_links where test_case_id = ?",
                Integer.class,
                caseId))
        .isEqualTo(1);

    mockMvc
        .perform(
            get("/api/v1/generation-runs/{runId}", runId)
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providerAdapterVersion").value("legacy-unknown"))
        .andExpect(jsonPath("$.resultContractVersion").value("legacy-unknown"))
        .andExpect(jsonPath("$.schemaVersion").value("manual-test-schema-v1"))
        .andExpect(jsonPath("$.validatorVersion").value("legacy-unknown"))
        .andExpect(jsonPath("$.sourceSnapshotProvenance").value("LEGACY_RECONSTRUCTED"));
    mockMvc
        .perform(
            get("/api/v1/user-stories/{requirementId}/traceability", requirementId)
                .param("generationRunId", runId.toString())
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].provenance").value("LEGACY_RECONSTRUCTED"))
        .andExpect(jsonPath("$.rows[0].testCases[0].id").value(caseId.toString()));
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.generation_criterion_snapshots where generation_run_id = ?",
                Integer.class,
                runId))
        .isPositive();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.snapshot_traceability_links where test_case_id = ?",
                Integer.class,
                caseId))
        .isEqualTo(1);
  }

  /**
   * Transfers a legacy reopen reason to controlled revision history and scrubs broad audit data.
   */
  @Test
  @Order(12)
  void reconcilesLegacyReopenReasonWithoutDiscardingItsEvidence() throws Exception {
    UUID ownerId = users.findByEmailNormalized("owner@testforge.local").orElseThrow().getId();
    UUID pendingEventId = null;
    Instant bridgeTimestamp = Instant.parse("2026-08-05T20:05:00Z");
    for (int index = 0; index <= 100; index++) {
      UUID eventId = UUID.randomUUID();
      pendingEventId = eventId;
      jdbcTemplate.update(
          "insert into testforge.audit_events (id, actor_id, project_id, entity_type, entity_id, action, metadata, event_timestamp, correlation_id) values (?, ?, ?, 'TEST_CASE', ?, 'REOPENED', ?, ?, ?)",
          eventId,
          ownerId,
          UUID.fromString(projectId),
          UUID.fromString(testCaseId),
          "{\"testCaseKey\":\"legacy-case\",\"reason\":\"New evidence "
              + index
              + " requires controlled review.\"}",
          bridgeTimestamp.plusSeconds(index),
          "legacy-reopen-correlation-" + index);
    }

    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .param("entityType", "TEST_CASE")
                .param("entityId", testCaseId)
                .param("action", "REOPENED")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].metadata.reason").doesNotExist())
        .andExpect(jsonPath("$.items[0].metadata.legacyReasonMigrated").doesNotExist())
        .andExpect(jsonPath("$.items[0].metadata.legacyReasonRedacted").value(true))
        .andExpect(jsonPath("$.items[0].metadata.pendingReconciliation").value(true));
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.audit_events where correlation_id like 'legacy-reopen-correlation-%' and metadata like '%\"reason\"%'",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.test_case_revisions r join testforge.audit_events a on a.id = r.source_audit_event_id where a.correlation_id like 'legacy-reopen-correlation-%'",
                Integer.class))
        .isEqualTo(100);

    UUID interleavedEventId = UUID.randomUUID();
    jdbcTemplate.update(
        "insert into testforge.audit_events (id, actor_id, project_id, entity_type, entity_id, action, metadata, event_timestamp, correlation_id) values (?, ?, ?, 'TEST_CASE', ?, 'REOPENED', ?, ?, 'legacy-reopen-interleaved')",
        interleavedEventId,
        ownerId,
        UUID.fromString(projectId),
        UUID.fromString(testCaseId),
        "{\"testCaseKey\":\"legacy-case\",\"reason\":\"Interleaved bridge write.\"}",
        bridgeTimestamp.plusSeconds(200));

    MvcResult revisions =
        mockMvc
            .perform(
                get("/api/v1/test-cases/{testCaseId}/revisions", testCaseId)
                    .param("size", "100")
                    .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(json(revisions).path("items").findValuesAsText("changeType"))
        .contains("LEGACY_REOPEN");
    assertThat(json(revisions).path("items").findValuesAsText("changeReason"))
        .contains("New evidence 100 requires controlled review.", "Interleaved bridge write.");

    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .param("entityType", "TEST_CASE")
                .param("entityId", testCaseId)
                .param("action", "REOPENED")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].metadata.reason").doesNotExist())
        .andExpect(jsonPath("$.items[0].metadata.legacyReasonMigrated").value(true));
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.test_case_revisions where source_audit_event_id in (?, ?)",
                Integer.class,
                pendingEventId,
                interleavedEventId))
        .isEqualTo(2);

    UUID orphanCaseId = UUID.randomUUID();
    UUID orphanEventId = UUID.randomUUID();
    jdbcTemplate.update(
        "insert into testforge.audit_events (id, actor_id, project_id, entity_type, entity_id, action, metadata, event_timestamp, correlation_id) values (?, ?, ?, 'TEST_CASE', ?, 'REOPENED', ?, ?, 'legacy-reopen-orphan')",
        orphanEventId,
        ownerId,
        UUID.fromString(projectId),
        orphanCaseId,
        "{\"testCaseKey\":\"missing-case\",\"reason\":\"Preserve for operator repair.\"}",
        bridgeTimestamp.plusSeconds(300));

    mockMvc
        .perform(
            get("/api/v1/projects/{projectId}/audit-events", projectId)
                .param("entityType", "TEST_CASE")
                .param("entityId", orphanCaseId.toString())
                .param("action", "REOPENED")
                .header("Authorization", bearer(ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].metadata.legacyReasonMigrated").doesNotExist())
        .andExpect(jsonPath("$.items[0].metadata.legacyReasonRedacted").value(true))
        .andExpect(jsonPath("$.items[0].metadata.pendingReconciliation").value(true));
    assertThat(
            jdbcTemplate.queryForObject(
                "select metadata from testforge.audit_events where id = ?",
                String.class,
                orphanEventId))
        .contains("\"reason\":\"Preserve for operator repair.\"")
        .doesNotContain("legacyReasonMigrated");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.test_case_revisions where source_audit_event_id = ?",
                Integer.class,
                orphanEventId))
        .isZero();
  }

  /** Uses UUID as the documented deterministic tie-break when completion timestamps match. */
  @Test
  @Order(13)
  void numbersEqualCompletionTimesByUuid() {
    UUID ownerId = users.findByEmailNormalized("owner@testforge.local").orElseThrow().getId();
    UUID requirementUuid = UUID.fromString(requirementId);
    UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID higher = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Instant completed = Instant.parse("2026-08-05T21:00:00Z");
    insertCompletedRun(lower, requirementUuid, ownerId, "c".repeat(64), completed);
    insertCompletedRun(higher, requirementUuid, ownerId, "d".repeat(64), completed);

    Map<UUID, Long> numbers =
        generationRuns.findSetNumbersByRunIds(List.of(lower, higher)).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    item -> UUID.fromString(item.getRunId()),
                    com.testforge.generation.repository.GenerationRunRepository.GenerationSetNumber
                        ::getSetNumber));
    assertThat(numbers.get(higher)).isEqualTo(numbers.get(lower) + 1);
  }

  /**
   * Keeps missing, invalid, expired, unavailable-account, and replay failures indistinguishable.
   */
  @Test
  @Order(14)
  void returnsOneGenericRefreshFailureContractForEveryFailureClass() throws Exception {
    List<MvcResult> failures = new java.util.ArrayList<>();
    failures.add(mockMvc.perform(post("/api/v1/auth/refresh").with(csrf())).andReturn());
    failures.add(
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .with(csrf())
                    .cookie(new Cookie("testforge_refresh", "invalid-refresh-token")))
            .andReturn());

    MvcResult expiredRegistration =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"expired-refresh@testforge.local\",\"displayName\":\"Expired Refresh\",\"password\":\"TestForge!Expired2026\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie expiredCookie = expiredRegistration.getResponse().getCookie("testforge_refresh");
    UUID expiredUser = UUID.fromString(json(expiredRegistration).path("user").path("id").asText());
    jdbcTemplate.update(
        "update testforge.refresh_token_sessions set expires_at = ? where user_id = ?",
        Instant.parse("2020-01-01T00:00:00Z"),
        expiredUser);
    failures.add(
        mockMvc
            .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(expiredCookie))
            .andReturn());

    MvcResult disabledRegistration =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"disabled-refresh@testforge.local\",\"displayName\":\"Disabled Refresh\",\"password\":\"TestForge!Disabled2026\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie disabledCookie = disabledRegistration.getResponse().getCookie("testforge_refresh");
    UUID disabledUser =
        UUID.fromString(json(disabledRegistration).path("user").path("id").asText());
    jdbcTemplate.update("update testforge.users set enabled = false where id = ?", disabledUser);
    failures.add(
        mockMvc
            .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(disabledCookie))
            .andReturn());

    for (MvcResult failure : failures) {
      assertThat(failure.getResponse().getStatus()).isEqualTo(401);
      JsonNode problem = json(failure);
      assertThat(problem.path("code").asText()).isEqualTo("authentication_failed");
      assertThat(problem.path("detail").asText())
          .isEqualTo("The session is invalid. Sign in again.");
      assertThat(failure.getResponse().getHeader("Set-Cookie"))
          .contains("testforge_refresh=")
          .contains("Max-Age=0");
    }
  }

  /** Keeps a valid refresh cookie intact when the request is rejected before authentication. */
  @Test
  @Order(15)
  void rateLimitedRefreshDoesNotClearTheValidCookie() throws Exception {
    MvcResult registration =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"limited-refresh@testforge.local\",\"displayName\":\"Limited Refresh\",\"password\":\"TestForge!Limited2026\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    Cookie validCookie = registration.getResponse().getCookie("testforge_refresh");
    assertThat(validCookie).isNotNull();

    for (int index = 0; index < 100; index++) {
      mockMvc
          .perform(
              post("/api/v1/auth/refresh")
                  .with(csrf())
                  .with(
                      request -> {
                        request.setRemoteAddr("198.51.100.200");
                        return request;
                      })
                  .cookie(new Cookie("testforge_refresh", "invalid-limited-token-" + index)))
          .andExpect(status().isUnauthorized());
    }

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .with(csrf())
                .with(
                    request -> {
                      request.setRemoteAddr("198.51.100.200");
                      return request;
                    })
                .cookie(validCookie))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().doesNotExist("Set-Cookie"));
  }

  /** Rejects fenced provider output without persisting any partial generated evidence graph. */
  @Test
  @Order(16)
  void rejectsCodeFenceOutputWithoutPartialPersistence() throws Exception {
    reset(generationProvider);
    doAnswer(
            invocation ->
                withUnsafeCodeFence(
                    new FakeTestGenerationProvider()
                        .generate(invocation.getArgument(0, TestGenerationRequest.class))))
        .when(generationProvider)
        .generate(any());
    try {
      JsonNode rejected =
          json(
              mockMvc
                  .perform(
                      post("/api/v1/user-stories/{requirementId}/regenerate", requirementId)
                          .with(csrf())
                          .param("confirmSupersede", "true")
                          .header("Authorization", bearer(ownerToken))
                          .header("Idempotency-Key", "unsafe-code-fence-generation"))
                  .andExpect(status().isCreated())
                  .andExpect(jsonPath("$.status").value("REJECTED_BY_VALIDATION"))
                  .andReturn());
      UUID rejectedRunId = UUID.fromString(rejected.path("id").asText());
      assertTerminalRunHasNoPartialGeneratedGraph(rejectedRunId);
    } finally {
      reset(generationProvider);
    }
  }

  /** Rejects a missing automation-candidate decision without persisting generated case evidence. */
  @Test
  @Order(17)
  void rejectsMissingAutomationCandidateWithoutPartialPersistence() throws Exception {
    reset(generationProvider);
    doAnswer(
            invocation ->
                withMissingAutomationCandidate(
                    new FakeTestGenerationProvider()
                        .generate(invocation.getArgument(0, TestGenerationRequest.class))))
        .when(generationProvider)
        .generate(any());
    try {
      JsonNode rejected =
          json(
              mockMvc
                  .perform(
                      post("/api/v1/user-stories/{requirementId}/regenerate", requirementId)
                          .with(csrf())
                          .param("confirmSupersede", "true")
                          .header("Authorization", bearer(ownerToken))
                          .header("Idempotency-Key", "missing-automation-candidate"))
                  .andExpect(status().isCreated())
                  .andExpect(jsonPath("$.status").value("REJECTED_BY_VALIDATION"))
                  .andReturn());
      assertTerminalRunHasNoPartialGeneratedGraph(UUID.fromString(rejected.path("id").asText()));
    } finally {
      reset(generationProvider);
    }
  }

  /** Reconciles a same-key V5-shaped POST response without invoking the provider again. */
  @Test
  @Order(18)
  void reconcilesExistingBridgeRunOnGenerationPost() throws Exception {
    UUID ownerId = users.findByEmailNormalized("owner@testforge.local").orElseThrow().getId();
    String idempotencyKey = "existing-bridge-generation";
    UUID runId = UUID.randomUUID();
    insertCompletedRun(
        runId,
        UUID.fromString(requirementId),
        ownerId,
        sha256(idempotencyKey),
        Instant.parse("2026-08-05T22:00:00Z"));
    reset(generationProvider);

    mockMvc
        .perform(
            post("/api/v1/user-stories/{requirementId}/generate-test-cases", requirementId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .header("Idempotency-Key", idempotencyKey))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(runId.toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.providerAdapterVersion").value("legacy-unknown"))
        .andExpect(jsonPath("$.resultContractVersion").value("legacy-unknown"))
        .andExpect(jsonPath("$.schemaVersion").value("manual-test-schema-v1"))
        .andExpect(jsonPath("$.validatorVersion").value("legacy-unknown"))
        .andExpect(jsonPath("$.sourceSnapshotProvenance").value("LEGACY_RECONSTRUCTED"));
    verify(generationProvider, times(0)).generate(any());
  }

  /** Preserves the captured criterion identity when its key changes during provider work. */
  @Test
  @Order(19)
  void dualWritesLegacyTraceabilityByIdentityAfterInFlightCriterionRename() throws Exception {
    JsonNode source = ownedRequirement();
    JsonNode criterion = source.path("acceptanceCriteria").get(0);
    UUID criterionId = UUID.fromString(criterion.path("id").asText());
    ProviderGate gate = blockProvider();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<MvcResult> generation = submitGeneration(executor, "in-flight-criterion-rename");
      assertThat(gate.entered().await(10, TimeUnit.SECONDS)).isTrue();

      mockMvc
          .perform(
              patch("/api/v1/acceptance-criteria/{criterionId}", criterionId)
                  .with(csrf())
                  .header("Authorization", bearer(ownerToken))
                  .header("If-Match", '"' + source.path("version").asText() + '"')
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "criterionKey",
                              "AC-9",
                              "description",
                              criterion.path("description").asText(),
                              "sortOrder",
                              criterion.path("sortOrder").asInt()))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(criterionId.toString()))
          .andExpect(jsonPath("$.criterionKey").value("AC-9"));

      gate.release().countDown();
      JsonNode completed = json(generation.get(10, TimeUnit.SECONDS));
      UUID runId = UUID.fromString(completed.path("id").asText());
      assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
      assertThat(
              jdbcTemplate.queryForObject(
                  "select count(*) from testforge.traceability_links l join testforge.test_cases c on c.id = l.test_case_id where c.generation_run_id = ? and l.acceptance_criterion_id = ?",
                  Integer.class,
                  runId,
                  criterionId))
          .isPositive();
      assertThat(
              jdbcTemplate.queryForObject(
                  "select count(*) from testforge.generation_criterion_snapshots where generation_run_id = ? and source_acceptance_criterion_id = ? and criterion_key = 'AC-1'",
                  Integer.class,
                  runId,
                  criterionId))
          .isEqualTo(1);
    } finally {
      gate.release().countDown();
      executor.shutdownNow();
      reset(generationProvider);
    }
  }

  /** Fails atomically when a captured criterion is deleted during provider work. */
  @Test
  @Order(20)
  void failsWithoutCasesAfterInFlightCriterionDeletion() throws Exception {
    JsonNode source = ownedRequirement();
    UUID criterionId =
        UUID.fromString(source.path("acceptanceCriteria").get(0).path("id").asText());
    ProviderGate gate = blockProvider();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<MvcResult> generation = submitGeneration(executor, "in-flight-criterion-deletion");
      assertThat(gate.entered().await(10, TimeUnit.SECONDS)).isTrue();

      mockMvc
          .perform(
              delete("/api/v1/acceptance-criteria/{criterionId}", criterionId)
                  .with(csrf())
                  .header("Authorization", bearer(ownerToken))
                  .header("If-Match", '"' + source.path("version").asText() + '"'))
          .andExpect(status().isNoContent());

      gate.release().countDown();
      JsonNode failed = json(generation.get(10, TimeUnit.SECONDS));
      UUID runId = UUID.fromString(failed.path("id").asText());
      assertThat(failed.path("status").asText()).isEqualTo("FAILED");
      assertThat(failed.path("failureCode").asText()).isEqualTo("source_criteria_changed");
      assertThat(failed.path("failureMessage").asText())
          .isEqualTo("Source acceptance criteria changed while generation was in progress.");
      assertTerminalRunHasNoPartialGeneratedGraph(runId);
    } finally {
      gate.release().countDown();
      executor.shutdownNow();
      reset(generationProvider);
    }
  }

  /** Rejects whitespace-only idempotency keys before either generation path reaches a provider. */
  @Test
  @Order(21)
  void rejectsBlankGenerationIdempotencyKeysBeforeProviderInvocation() throws Exception {
    reset(generationProvider);

    mockMvc
        .perform(
            post("/api/v1/user-stories/{requirementId}/generate-test-cases", requirementId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .header("Idempotency-Key", "        "))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/user-stories/{requirementId}/regenerate", requirementId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .header("Idempotency-Key", "        "))
        .andExpect(status().isBadRequest());

    verify(generationProvider, times(0)).generate(any());
  }

  /** Injects one fenced rationale while preserving an otherwise valid provider result. */
  private TestGenerationResult withUnsafeCodeFence(TestGenerationResult valid) {
    GeneratedTestCase base = valid.testCases().getFirst();
    GeneratedTestCase unsafe =
        new GeneratedTestCase(
            base.title(),
            base.objective(),
            base.category(),
            base.priority(),
            base.riskLevel(),
            base.automationCandidate(),
            base.coverageIntent(),
            base.preconditions(),
            base.testData(),
            base.steps(),
            base.finalExpectedOutcome(),
            base.acceptanceCriteriaKeys(),
            "```javascript\nfetch('https://example.invalid')\n```");
    List<GeneratedTestCase> cases = new java.util.ArrayList<>(valid.testCases());
    cases.set(0, unsafe);
    return new TestGenerationResult(
        valid.requirementSummary(), valid.ambiguities(), cases, valid.usage());
  }

  /** Removes one required automation-candidate value from an otherwise valid candidate. */
  private TestGenerationResult withMissingAutomationCandidate(TestGenerationResult valid) {
    GeneratedTestCase base = valid.testCases().getFirst();
    GeneratedTestCase missing =
        new GeneratedTestCase(
            base.title(),
            base.objective(),
            base.category(),
            base.priority(),
            base.riskLevel(),
            null,
            base.coverageIntent(),
            base.preconditions(),
            base.testData(),
            base.steps(),
            base.finalExpectedOutcome(),
            base.acceptanceCriteriaKeys(),
            base.rationale());
    List<GeneratedTestCase> cases = new java.util.ArrayList<>(valid.testCases());
    cases.set(0, missing);
    return new TestGenerationResult(
        valid.requirementSummary(), valid.ambiguities(), cases, valid.usage());
  }

  /** Proves a safe terminal generation retains only claim-time source snapshots. */
  private void assertTerminalRunHasNoPartialGeneratedGraph(UUID rejectedRunId) {
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.test_cases where generation_run_id = ?",
                Integer.class,
                rejectedRunId))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.traceability_links l join testforge.test_cases c on c.id = l.test_case_id where c.generation_run_id = ?",
                Integer.class,
                rejectedRunId))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.snapshot_traceability_links l join testforge.test_cases c on c.id = l.test_case_id where c.generation_run_id = ?",
                Integer.class,
                rejectedRunId))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from testforge.generation_criterion_snapshots where generation_run_id = ?",
                Integer.class,
                rejectedRunId))
        .isPositive();
  }

  /** Loads the current owner-scoped User Story aggregate for mutation-race assertions. */
  private JsonNode ownedRequirement() throws Exception {
    return json(
        mockMvc
            .perform(
                get("/api/v1/user-stories/{requirementId}", requirementId)
                    .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andReturn());
  }

  /** Blocks the deterministic provider after claim commit until a test releases it. */
  private ProviderGate blockProvider() throws Exception {
    reset(generationProvider);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              entered.countDown();
              if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Provider latch timed out.");
              }
              return invocation.callRealMethod();
            })
        .when(generationProvider)
        .generate(any());
    return new ProviderGate(entered, release);
  }

  /** Starts one generation request on a worker so source criteria can change in flight. */
  private Future<MvcResult> submitGeneration(ExecutorService executor, String idempotencyKey) {
    return executor.submit(
        () ->
            mockMvc
                .perform(
                    post("/api/v1/user-stories/{requirementId}/generate-test-cases", requirementId)
                        .with(csrf())
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andReturn());
  }

  /** Hashes the raw synthetic idempotency key exactly as the generation service does. */
  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }

  /** Synchronizes deterministic provider entry and release in mutation-race tests. */
  private record ProviderGate(CountDownLatch entered, CountDownLatch release) {}

  /** Inserts one completed bridge-shaped run for deterministic repository ordering tests. */
  private void insertCompletedRun(
      UUID runId, UUID requirementUuid, UUID ownerId, String idempotencyHash, Instant completed) {
    jdbcTemplate.update(
        "insert into testforge.generation_runs (id, requirement_id, requested_by, provider, model, prompt_version, status, input_hash, idempotency_key_hash, started_at, completed_at, latency_ms, generated_case_count, input_tokens, output_tokens, correlation_id) values (?, ?, ?, 'legacy-provider', 'legacy-model', 'legacy-prompt', 'COMPLETED', ?, ?, ?, ?, 10, 0, 1, 1, 'tie-correlation')",
        runId,
        requirementUuid,
        ownerId,
        "e".repeat(64),
        idempotencyHash,
        completed.minusSeconds(1),
        completed);
  }

  /** Compares prepared-statement counts for equivalent small and large page reads. */
  private void assertConstantStatementCount(
      Statistics statistics, Supplier<?> smallPage, Supplier<?> largePage) {
    statistics.clear();
    smallPage.get();
    long smallCount = statistics.getPrepareStatementCount();
    statistics.clear();
    largePage.get();
    long largeCount = statistics.getPrepareStatementCount();
    assertThat(largeCount).isLessThanOrEqualTo(smallCount).isLessThanOrEqualTo(10);
  }

  /** Asserts that an unconfirmed generation alias cannot supersede protected evidence. */
  private void assertSupersessionConfirmationRequired(String path, String idempotencyKey)
      throws Exception {
    mockMvc
        .perform(
            post(path, requirementId)
                .with(csrf())
                .header("Authorization", bearer(ownerToken))
                .header("Idempotency-Key", idempotencyKey))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("supersede_confirmation_required"));
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

  /** Copies the mutable fields of a test-case response into an update request. */
  private ObjectNode editableUpdate(JsonNode existing) {
    ObjectNode update = objectMapper.createObjectNode();
    update.put("title", existing.get("title").asText());
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
    update.set("steps", existing.get("steps").deepCopy());
    update.set("testData", existing.get("testData").deepCopy());
    update.put("version", existing.get("version").asLong());
    return update;
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
