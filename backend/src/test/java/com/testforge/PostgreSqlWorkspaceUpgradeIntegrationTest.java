package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlWorkspaceUpgradeIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(PostgreSqlTestImage.POSTGRES)
          .withDatabaseName("testforge_workspace_upgrade")
          .withUsername("testforge_app")
          .withPassword("integration-only-password");

  /**
   * Proves V3-to-V6 upgrades preserve owner mapping and backfill legacy priority deterministically.
   */
  @Test
  void upgradesLegacyUsersAndProjectsThroughV4WithoutCrossUserMapping() {
    configuredFlyway(MigrationVersion.fromVersion("3")).migrate();
    JdbcTemplate jdbc = jdbc();
    UUID firstUserId = UUID.randomUUID();
    UUID secondUserId = UUID.randomUUID();
    UUID firstProjectId = UUID.randomUUID();
    UUID secondProjectId = UUID.randomUUID();
    insertLegacyUser(jdbc, firstUserId, "first-upgrade@testforge.local");
    insertLegacyUser(jdbc, secondUserId, "second-upgrade@testforge.local");
    insertLegacyProject(jdbc, firstProjectId, firstUserId, "First Legacy Project");
    insertLegacyProject(jdbc, secondProjectId, secondUserId, "Second Legacy Project");

    configuredFlyway(MigrationVersion.fromVersion("4")).migrate();
    UUID requirementId = UUID.randomUUID();
    jdbc.update(
        "insert into testforge.requirements (id, work_item_number, project_id, title, user_story, business_requirements, assumptions, source_reference, status, created_by, created_at, updated_at, version) values (?, 9901, ?, 'Legacy PostgreSQL story', 'As a tester, I need deterministic priority.', '', '', '', 'DRAFT', ?, current_timestamp, current_timestamp, 0)",
        requirementId,
        firstProjectId,
        firstUserId);
    configuredFlyway(null).migrate();

    List<String> appliedVersions =
        jdbc.queryForList(
            "select version from testforge.flyway_schema_history where success and version is not null order by installed_rank",
            String.class);
    assertThat(appliedVersions).containsExactly("1", "2", "3", "4", "5", "6");
    assertThat(
            jdbc.queryForObject(
                "select priority from testforge.requirements where id = ?",
                String.class,
                requirementId))
        .isEqualTo("MEDIUM");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.workspaces where id = created_by and status = 'ACTIVE'",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.workspace_memberships where id = user_id and workspace_id = user_id and created_by = user_id and role = 'OWNER' and status = 'ACTIVE'",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.projects where workspace_id = owner_id",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.projects where workspace_id <> owner_id",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.workspace_memberships m left join testforge.workspaces w on w.id = m.workspace_id where w.id is null",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select workspace_id from testforge.projects where id = ?",
                UUID.class,
                firstProjectId))
        .isEqualTo(firstUserId);
    assertThat(
            jdbc.queryForObject(
                "select workspace_id from testforge.projects where id = ?",
                UUID.class,
                secondProjectId))
        .isEqualTo(secondUserId);
  }

  /** Uses an optional historical target to model prior-binary rows before upgrading. */
  private Flyway configuredFlyway(MigrationVersion target) {
    FluentConfiguration configuration =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .schemas("testforge")
            .defaultSchema("testforge")
            .createSchemas(true)
            .locations("classpath:db/migration");
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  /** Creates a JDBC facade for assertions and legacy-fixture insertion. */
  private JdbcTemplate jdbc() {
    return new JdbcTemplate(
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
  }

  /** Inserts a user using only columns available before the workspace migration. */
  private void insertLegacyUser(JdbcTemplate jdbc, UUID userId, String email) {
    jdbc.update(
        "insert into testforge.users (id, email, email_normalized, display_name, password_hash, role, enabled, created_at, updated_at) values (?, ?, ?, 'Legacy Upgrade User', 'not-a-real-hash', 'USER', true, current_timestamp, current_timestamp)",
        userId,
        email,
        email);
  }

  /** Inserts a project using the V3 owner-only tenancy contract. */
  private void insertLegacyProject(JdbcTemplate jdbc, UUID projectId, UUID ownerId, String name) {
    jdbc.update(
        "insert into testforge.projects (id, owner_id, name, description, status, created_at, updated_at, version) values (?, ?, ?, '', 'ACTIVE', current_timestamp, current_timestamp, 0)",
        projectId,
        ownerId,
        name);
  }
}
