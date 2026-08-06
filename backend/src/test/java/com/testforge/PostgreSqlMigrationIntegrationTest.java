package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testforge.generation.provider.FakeTestGenerationProvider;
import jakarta.servlet.http.Cookie;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(PostgreSqlTestImage.POSTGRES)
          .withDatabaseName("testforge")
          .withUsername("testforge_app")
          .withPassword("integration-only-password");

  /** Binds Spring to the container so Flyway and JPA exercise PostgreSQL rather than H2. */
  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    registry.add("testforge.auth.issuer", () -> "testforge-postgresql-integration");
    registry.add(
        "testforge.auth.access-token-secret",
        () -> "VGVzdEZvcmdlLXBvc3RncmVzcWwtaW50ZWdyYXRpb24ta2V5LTMyLWJ5dGVz");
    registry.add("testforge.auth.access-token-ttl", () -> "PT10M");
    registry.add("testforge.auth.refresh-token-ttl", () -> "P7D");
    registry.add("testforge.auth.secure-cookies", () -> "false");
    registry.add("testforge.auth.refresh-cookie-name", () -> "testforge_refresh");
    registry.add("testforge.security.allowed-origins[0]", () -> "http://localhost:5173");
    registry.add("testforge.security.auth-attempts-per-minute", () -> "100");
    registry.add("testforge.security.generation-attempts-per-minute", () -> "100");
    registry.add("testforge.generation.provider", () -> "fake");
    registry.add("testforge.generation.maximum-cases", () -> "25");
    registry.add("testforge.generation.maximum-steps-per-case", () -> "30");
    registry.add("testforge.demo.seed-enabled", () -> "false");
  }

  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoSpyBean private FakeTestGenerationProvider generationProvider;

  /** Proves fresh PostgreSQL applies every migration and retains rollback-safe nullable bridges. */
  @Test
  void flywayCreatesTheNormalizedSchemaWithPostgreSqlConstraintsAndUtcTypes() {
    List<String> appliedVersions =
        jdbc.queryForList(
            "select version from testforge.flyway_schema_history where success and version is not null order by installed_rank",
            String.class);
    Integer domainTables =
        jdbc.queryForObject(
            "select count(*) from information_schema.tables where table_schema = 'testforge'",
            Integer.class);
    String timestampType =
        jdbc.queryForObject(
            "select data_type from information_schema.columns where table_schema = 'testforge' and table_name = 'users' and column_name = 'created_at'",
            String.class);
    String projectWorkspaceNullable =
        jdbc.queryForObject(
            "select is_nullable from information_schema.columns where table_schema = 'testforge' and table_name = 'projects' and column_name = 'workspace_id'",
            String.class);
    Integer priorityRollbackBridgeColumns =
        jdbc.queryForObject(
            "select count(*) from information_schema.columns where table_schema = 'testforge' and table_name = 'requirements' and column_name = 'priority' and is_nullable = 'YES' and column_default is null",
            Integer.class);

    assertThat(appliedVersions).containsExactly("1", "2", "3", "4", "5", "6");
    assertThat(domainTables).isGreaterThanOrEqualTo(20);
    assertThat(timestampType).isEqualTo("timestamp with time zone");
    assertThat(projectWorkspaceNullable).isEqualTo("YES");
    assertThat(priorityRollbackBridgeColumns).isEqualTo(1);

    UUID ownerId = UUID.randomUUID();
    insertUser(ownerId, "case@testforge.local", "case@testforge.local");
    assertThatThrownBy(
            () -> insertUser(UUID.randomUUID(), "CASE@testforge.local", "case@testforge.local"))
        .isInstanceOf(DataIntegrityViolationException.class);

    insertWorkspace(ownerId);
    insertMembership(ownerId, ownerId, "OWNER");
    assertThatThrownBy(() -> insertMembership(ownerId, ownerId, "STAKEHOLDER"))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID secondUserId = UUID.randomUUID();
    insertUser(secondUserId, "role@testforge.local", "role@testforge.local");
    assertThatThrownBy(() -> insertMembership(ownerId, secondUserId, "UNRECOGNIZED_ROLE"))
        .isInstanceOf(DataIntegrityViolationException.class);
    insertProjectWithNullableWorkspace(UUID.randomUUID(), ownerId);
  }

  /** Proves PostgreSQL row locking prevents refresh-family forks and commits replay containment. */
  @Test
  void serializesConcurrentRefreshRotationAndRevokesTheReplayedFamily() throws Exception {
    MvcResult registration =
        register(
            "postgres-refresh@testforge.local", "PostgreSQL Refresh", "TestForge!PgRefresh2026");
    Cookie predecessor = registration.getResponse().getCookie("testforge_refresh");
    UUID userId = UUID.fromString(json(registration).path("user").path("id").asText());
    assertThat(predecessor).isNotNull();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<MvcResult> refresh =
        () -> {
          ready.countDown();
          start.await(10, TimeUnit.SECONDS);
          return mockMvc
              .perform(post("/api/v1/auth/refresh").with(csrf()).cookie(predecessor))
              .andReturn();
        };
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<MvcResult> left = executor.submit(refresh);
      Future<MvcResult> right = executor.submit(refresh);
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<MvcResult> results =
          List.of(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS));
      assertThat(results.stream().map(result -> result.getResponse().getStatus()).toList())
          .containsExactlyInAnyOrder(200, 401);
      MvcResult rejected =
          results.stream()
              .filter(result -> result.getResponse().getStatus() == 401)
              .findFirst()
              .orElseThrow();
      assertGenericRefreshFailure(rejected);

      UUID familyId =
          jdbc.queryForObject(
              "select family_id from testforge.refresh_token_sessions where user_id = ? order by created_at fetch first 1 row only",
              UUID.class,
              userId);
      Map<String, Object> evidence =
          jdbc.queryForMap(
              "select count(*) as token_count, count(*) filter (where revoked_at is not null) as revoked_count, count(*) filter (where reuse_detected) as reuse_count, count(*) filter (where replaced_by_token_id is not null) as lineage_count from testforge.refresh_token_sessions where family_id = ?",
              familyId);
      assertThat(((Number) evidence.get("token_count")).intValue()).isEqualTo(2);
      assertThat(((Number) evidence.get("revoked_count")).intValue()).isEqualTo(2);
      assertThat(((Number) evidence.get("reuse_count")).intValue()).isEqualTo(1);
      assertThat(((Number) evidence.get("lineage_count")).intValue()).isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from testforge.audit_events where actor_id = ? and action = 'TOKEN_REUSE_DETECTED'",
                  Integer.class,
                  userId))
          .isEqualTo(1);
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  /**
   * Proves PostgreSQL claim locking deduplicates a key while provider work remains
   * transaction-free.
   */
  @Test
  void deduplicatesConcurrentGenerationWithoutHoldingAPostgreSqlTransaction() throws Exception {
    Fixture fixture = createGenerationFixture();
    reset(generationProvider);
    CountDownLatch providerEntered = new CountDownLatch(1);
    CountDownLatch releaseProvider = new CountDownLatch(1);
    AtomicBoolean transactionActive = new AtomicBoolean(true);
    doAnswer(
            invocation -> {
              transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
              providerEntered.countDown();
              if (!releaseProvider.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Provider latch timed out.");
              }
              return invocation.callRealMethod();
            })
        .when(generationProvider)
        .generate(any());
    String key = "postgres-concurrent-generation";
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<MvcResult> first = executor.submit(() -> generate(fixture, key));
      assertThat(providerEntered.await(10, TimeUnit.SECONDS)).isTrue();
      assertThat(transactionActive.get()).isFalse();
      mockMvc
          .perform(
              get("/api/v1/projects/{projectId}", fixture.projectId())
                  .header("Authorization", bearer(fixture.accessToken())))
          .andExpect(status().isOk());
      MvcResult duplicate = generate(fixture, key);
      assertThat(duplicate.getResponse().getStatus()).isEqualTo(201);
      assertThat(json(duplicate).path("status").asText()).isEqualTo("PENDING");
      verify(generationProvider, times(1)).generate(any());
      releaseProvider.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(201);
      verify(generationProvider, times(1)).generate(any());
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from testforge.generation_runs where requirement_id = ?",
                  Integer.class,
                  fixture.requirementId()))
          .isEqualTo(1);
    } finally {
      releaseProvider.countDown();
      executor.shutdownNow();
      reset(generationProvider);
    }
  }

  /** Builds a complete owner/project/story fixture through public PostgreSQL-backed APIs. */
  private Fixture createGenerationFixture() throws Exception {
    MvcResult registration =
        register(
            "postgres-generation@testforge.local",
            "PostgreSQL Generation",
            "TestForge!PgGeneration2026");
    String token = json(registration).path("accessToken").asText();
    JsonNode project =
        json(
            mockMvc
                .perform(
                    post("/api/v1/projects")
                        .with(csrf())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of("name", "PostgreSQL claims", "description", "Synthetic"))))
                .andExpect(status().isCreated())
                .andReturn());
    String projectId = project.path("id").asText();
    JsonNode requirement =
        json(
            mockMvc
                .perform(
                    post("/api/v1/projects/{projectId}/user-stories", projectId)
                        .with(csrf())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                Map.of(
                                    "title", "Create one synthetic claim",
                                    "userStory", "As a tester, I want one synthetic claim.",
                                    "businessRequirements", "One claim is created exactly once.",
                                    "assumptions", "Synthetic records exist.",
                                    "sourceReference", "PG-CONCURRENCY",
                                    "priority", "HIGH",
                                    "acceptanceCriteria", List.of("One claim is created.")))))
                .andExpect(status().isCreated())
                .andReturn());
    return new Fixture(token, projectId, UUID.fromString(requirement.path("id").asText()));
  }

  /** Executes one generation request without asserting its transient or terminal state. */
  private MvcResult generate(Fixture fixture, String key) throws Exception {
    return mockMvc
        .perform(
            post(
                    "/api/v1/user-stories/{requirementId}/generate-test-cases",
                    fixture.requirementId())
                .with(csrf())
                .header("Authorization", bearer(fixture.accessToken()))
                .header("Idempotency-Key", key))
        .andReturn();
  }

  /** Registers one unique synthetic integration account. */
  private MvcResult register(String email, String displayName, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "email", email,
                            "displayName", displayName,
                            "password", password))))
        .andExpect(status().isCreated())
        .andReturn();
  }

  /** Asserts the single public refresh-failure contract and mandatory cookie clearing. */
  private void assertGenericRefreshFailure(MvcResult result) throws Exception {
    assertThat(json(result).path("code").asText()).isEqualTo("authentication_failed");
    assertThat(json(result).path("detail").asText())
        .isEqualTo("The session is invalid. Sign in again.");
    assertThat(result.getResponse().getHeader("Set-Cookie"))
        .contains("testforge_refresh=")
        .contains("Max-Age=0");
  }

  /** Parses one MVC response as JSON. */
  private JsonNode json(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray());
  }

  /** Formats a bearer authorization header. */
  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record Fixture(String accessToken, String projectId, UUID requirementId) {}

  /** Supplies normalized email explicitly so PostgreSQL uniqueness is exercised. */
  private void insertUser(UUID id, String email, String normalizedEmail) {
    jdbc.update(
        "insert into testforge.users (id, email, email_normalized, display_name, password_hash, role, enabled, created_at, updated_at) values (?, ?, ?, 'Integration User', 'not-a-real-hash', 'USER', true, current_timestamp, current_timestamp)",
        id,
        email,
        normalizedEmail);
  }

  /** Inserts a workspace owned by an existing integration-test user. */
  private void insertWorkspace(UUID ownerId) {
    jdbc.update(
        "insert into testforge.workspaces (id, name, status, created_by, created_at, updated_at, version) values (?, 'Integration Workspace', 'ACTIVE', ?, current_timestamp, current_timestamp, 0)",
        ownerId,
        ownerId);
  }

  /** Inserts a membership to exercise workspace uniqueness and role constraints. */
  private void insertMembership(UUID workspaceId, UUID userId, String role) {
    jdbc.update(
        "insert into testforge.workspace_memberships (id, workspace_id, user_id, role, status, created_by, created_at, updated_at, version) values (?, ?, ?, ?, 'ACTIVE', ?, current_timestamp, current_timestamp, 0)",
        UUID.randomUUID(),
        workspaceId,
        userId,
        role,
        workspaceId);
  }

  /**
   * Inserts a project without workspace_id to prove the mixed-version rollback bridge remains open.
   */
  private void insertProjectWithNullableWorkspace(UUID projectId, UUID ownerId) {
    jdbc.update(
        "insert into testforge.projects (id, owner_id, workspace_id, name, description, status, created_at, updated_at, version) values (?, ?, null, 'Rollback Bridge', '', 'ACTIVE', current_timestamp, current_timestamp, 0)",
        projectId,
        ownerId);
  }
}
