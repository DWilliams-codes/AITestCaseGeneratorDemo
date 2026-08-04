package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:18.4-trixie")
          .withDatabaseName("testforge")
          .withUsername("testforge_app")
          .withPassword("integration-only-password");

  /** Executes the database properties operation for PostgreSqlMigrationIntegrationTest. */
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

  /**
   * Covers the flyway creates the normalized schema with postgre sql constraints and utc types
   * scenario.
   */
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

    assertThat(appliedVersions).containsExactly("1", "2", "3", "4");
    assertThat(domainTables).isGreaterThanOrEqualTo(18);
    assertThat(timestampType).isEqualTo("timestamp with time zone");
    assertThat(projectWorkspaceNullable).isEqualTo("YES");

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

  /** Executes the insert user operation for PostgreSqlMigrationIntegrationTest. */
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
