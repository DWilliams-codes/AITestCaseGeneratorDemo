package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class WorkspaceMigrationIntegrationTest {
  /** Verifies that a fresh H2 database applies the complete workspace-aware schema. */
  @Test
  void migratesFreshDatabaseThroughWorkspaceFoundation() {
    String url = databaseUrl("fresh");
    Flyway flyway = configuredFlyway(url, null);

    flyway.migrate();
    JdbcTemplate jdbc = jdbc(url);

    List<String> appliedVersions =
        Arrays.stream(flyway.info().applied())
            .map(info -> info.getVersion().toString())
            .toList();
    assertThat(appliedVersions).containsExactly("1", "2", "3", "4");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'testforge' and table_name in ('workspaces', 'workspace_memberships')",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select is_nullable from information_schema.columns where table_schema = 'testforge' and table_name = 'projects' and column_name = 'workspace_id'",
                String.class))
        .isEqualToIgnoringCase("YES");
  }

  /** Verifies deterministic backfill when an existing V3 database advances to V4. */
  @Test
  void backfillsLegacyOwnersMembershipsAndProjectsDeterministically() {
    String url = databaseUrl("legacy");
    configuredFlyway(url, MigrationVersion.fromVersion("3")).migrate();
    JdbcTemplate jdbc = jdbc(url);
    UUID ownerId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    jdbc.update(
        "insert into testforge.users (id, email, email_normalized, display_name, password_hash, role, enabled, created_at, updated_at) values (?, 'legacy@testforge.local', 'legacy@testforge.local', 'Legacy Owner', 'not-a-real-hash', 'USER', true, current_timestamp, current_timestamp)",
        ownerId);
    jdbc.update(
        "insert into testforge.projects (id, owner_id, name, description, status, created_at, updated_at, version) values (?, ?, 'Legacy Project', '', 'ACTIVE', current_timestamp, current_timestamp, 0)",
        projectId,
        ownerId);

    configuredFlyway(url, null).migrate();

    assertThat(jdbc.queryForObject("select id from testforge.workspaces", UUID.class))
        .isEqualTo(ownerId);
    assertThat(jdbc.queryForObject("select id from testforge.workspace_memberships", UUID.class))
        .isEqualTo(ownerId);
    assertThat(
            jdbc.queryForObject(
                "select workspace_id from testforge.workspace_memberships where user_id = ?",
                UUID.class,
                ownerId))
        .isEqualTo(ownerId);
    assertThat(
            jdbc.queryForObject(
                "select role from testforge.workspace_memberships where user_id = ?",
                String.class,
                ownerId))
        .isEqualTo("OWNER");
    assertThat(
            jdbc.queryForObject(
                "select status from testforge.workspace_memberships where user_id = ?",
                String.class,
                ownerId))
        .isEqualTo("ACTIVE");
    assertThat(
            jdbc.queryForObject(
                "select workspace_id from testforge.projects where id = ?", UUID.class, projectId))
        .isEqualTo(ownerId);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.workspace_memberships m left join testforge.workspaces w on w.id = m.workspace_id where w.id is null",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.projects p left join testforge.workspaces w on w.id = p.workspace_id where p.workspace_id is not null and w.id is null",
                Integer.class))
        .isZero();
  }

  /** Creates an isolated H2 URL with PostgreSQL compatibility enabled. */
  private String databaseUrl(String scenario) {
    return "jdbc:h2:mem:workspace_"
        + scenario
        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
  }

  /** Builds Flyway for the application schema and optional historical target. */
  private Flyway configuredFlyway(String url, MigrationVersion target) {
    FluentConfiguration configuration =
        Flyway.configure()
            .dataSource(url, "sa", "")
            .schemas("testforge")
            .defaultSchema("testforge")
            .createSchemas(true)
            .locations("classpath:db/migration");
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  /** Creates a JDBC facade bound to the isolated migration database. */
  private JdbcTemplate jdbc(String url) {
    return new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
  }
}
