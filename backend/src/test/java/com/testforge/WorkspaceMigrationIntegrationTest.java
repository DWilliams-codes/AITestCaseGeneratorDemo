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

class WorkspaceMigrationIntegrationTest {
  /** Verifies that a fresh H2 database applies the complete workspace-aware schema. */
  @Test
  void migratesFreshDatabaseThroughWorkspaceFoundation() {
    String url = databaseUrl("fresh");
    Flyway flyway = configuredFlyway(url, null);

    flyway.migrate();
    JdbcTemplate jdbc = jdbc(url);

    List<String> appliedVersions =
        jdbc.queryForList(
            "select version from testforge.flyway_schema_history where success and version is not null order by installed_rank",
            String.class);
    assertThat(appliedVersions).containsExactly("1", "2", "3", "4", "5", "6");
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
    assertThat(
            jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'testforge' and table_name in ('generation_criterion_snapshots', 'snapshot_traceability_links')",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select is_nullable from information_schema.columns where table_schema = 'testforge' and table_name = 'requirements' and column_name = 'priority'",
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

  /** Verifies a V4 user story receives deterministic MEDIUM priority during V5 upgrade. */
  @Test
  void backfillsLegacyUserStoryPriorityWithoutMakingTheBridgeColumnRequired() {
    String url = databaseUrl("priority_upgrade");
    configuredFlyway(url, MigrationVersion.fromVersion("4")).migrate();
    JdbcTemplate jdbc = jdbc(url);
    UUID ownerId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID requirementId = UUID.randomUUID();
    jdbc.update(
        "insert into testforge.users (id, email, email_normalized, display_name, password_hash, role, enabled, created_at, updated_at) values (?, 'priority@testforge.local', 'priority@testforge.local', 'Priority Owner', 'not-a-real-hash', 'USER', true, current_timestamp, current_timestamp)",
        ownerId);
    jdbc.update(
        "insert into testforge.workspaces (id, name, status, created_by, created_at, updated_at, version) values (?, 'Priority Workspace', 'ACTIVE', ?, current_timestamp, current_timestamp, 0)",
        ownerId,
        ownerId);
    jdbc.update(
        "insert into testforge.projects (id, owner_id, workspace_id, name, description, status, created_at, updated_at, version) values (?, ?, ?, 'Priority Project', '', 'ACTIVE', current_timestamp, current_timestamp, 0)",
        projectId,
        ownerId,
        ownerId);
    jdbc.update(
        "insert into testforge.requirements (id, work_item_number, project_id, title, user_story, business_requirements, assumptions, source_reference, status, created_by, created_at, updated_at, version) values (?, 9001, ?, 'Legacy story', 'As a tester, I need priority backfill.', '', '', '', 'DRAFT', ?, current_timestamp, current_timestamp, 0)",
        requirementId,
        projectId,
        ownerId);

    configuredFlyway(url, null).migrate();

    assertThat(
            jdbc.queryForObject(
                "select priority from testforge.requirements where id = ?",
                String.class,
                requirementId))
        .isEqualTo("MEDIUM");
    assertThat(
            jdbc.queryForObject(
                "select is_nullable from information_schema.columns where table_schema = 'testforge' and table_name = 'requirements' and column_name = 'priority'",
                String.class))
        .isEqualToIgnoringCase("YES");
  }

  /** Reconstructs V5 generation evidence while preserving old-binary nullable inserts. */
  @Test
  void reconstructsV5EvidenceAndKeepsTheRollbackBridgeNullable() {
    String url = databaseUrl("generation_evidence_upgrade");
    configuredFlyway(url, MigrationVersion.fromVersion("5")).migrate();
    JdbcTemplate jdbc = jdbc(url);
    UUID ownerId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID requirementId = UUID.randomUUID();
    UUID criterionId = UUID.randomUUID();
    UUID runId = UUID.randomUUID();
    UUID testCaseId = UUID.randomUUID();
    UUID linkId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    jdbc.update(
        "insert into testforge.users (id, email, email_normalized, display_name, password_hash, role, enabled, created_at, updated_at) values (?, 'evidence@testforge.local', 'evidence@testforge.local', 'Evidence Owner', 'not-a-real-hash', 'USER', true, current_timestamp, current_timestamp)",
        ownerId);
    jdbc.update(
        "insert into testforge.workspaces (id, name, status, created_by, created_at, updated_at, version) values (?, 'Evidence Workspace', 'ACTIVE', ?, current_timestamp, current_timestamp, 0)",
        ownerId,
        ownerId);
    jdbc.update(
        "insert into testforge.workspace_memberships (id, workspace_id, user_id, role, status, created_by, created_at, updated_at, version) values (?, ?, ?, 'OWNER', 'ACTIVE', ?, current_timestamp, current_timestamp, 0)",
        ownerId,
        ownerId,
        ownerId,
        ownerId);
    jdbc.update(
        "insert into testforge.projects (id, owner_id, workspace_id, name, description, status, created_at, updated_at, version) values (?, ?, ?, 'Evidence Project', '', 'ACTIVE', current_timestamp, current_timestamp, 0)",
        projectId,
        ownerId,
        ownerId);
    jdbc.update(
        "insert into testforge.requirements (id, project_id, title, user_story, business_requirements, assumptions, source_reference, status, priority, created_by, created_at, updated_at, version) values (?, ?, 'Evidence story', 'As a reviewer, I need stable history.', '', '', '', 'GENERATED', 'MEDIUM', ?, current_timestamp, current_timestamp, 7)",
        requirementId,
        projectId,
        ownerId);
    jdbc.update(
        "insert into testforge.acceptance_criteria (id, requirement_id, criterion_key, description, sort_order, created_at, updated_at) values (?, ?, 'AC-1', 'Original criterion text', 0, current_timestamp, current_timestamp)",
        criterionId,
        requirementId);
    insertV5GenerationRun(jdbc, runId, requirementId, ownerId, "legacy-key");
    jdbc.update(
        "insert into testforge.test_cases (id, requirement_id, generation_run_id, test_case_key, title, objective, category, priority, risk_level, automation_candidate, status, coverage_intent, rationale, final_expected_outcome, created_by, created_at, updated_at, version) values (?, ?, ?, 'TC-legacy-evidence', 'Legacy case', 'Verify legacy evidence', 'FUNCTIONAL', 'MEDIUM', 'MEDIUM', false, 'APPROVED', 'ACCEPTANCE_CRITERIA', 'Legacy rationale', 'Legacy outcome', ?, current_timestamp, current_timestamp, 0)",
        testCaseId,
        requirementId,
        runId,
        ownerId);
    jdbc.update(
        "insert into testforge.traceability_links (id, acceptance_criterion_id, test_case_id, coverage_type, confidence, created_at) values (?, ?, ?, 'DIRECT', 1.0000, current_timestamp)",
        linkId,
        criterionId,
        testCaseId);
    jdbc.update(
        "insert into testforge.test_case_revisions (id, test_case_id, revision_number, snapshot_json, changed_by, changed_at) values (?, ?, 1, '{}', ?, current_timestamp)",
        revisionId,
        testCaseId,
        ownerId);

    configuredFlyway(url, null).migrate();

    assertThat(
            jdbc.queryForObject(
                "select result_contract_version from testforge.generation_runs where id = ?",
                String.class,
                runId))
        .isEqualTo("legacy-unknown");
    assertThat(
            jdbc.queryForObject(
                "select source_snapshot_provenance from testforge.generation_runs where id = ?",
                String.class,
                runId))
        .isEqualTo("LEGACY_RECONSTRUCTED");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.generation_criterion_snapshots where generation_run_id = ? and criterion_key = 'AC-1' and description = 'Original criterion text' and source_requirement_version = 7 and provenance = 'LEGACY_RECONSTRUCTED'",
                Integer.class,
                runId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.snapshot_traceability_links link join testforge.generation_criterion_snapshots snapshot on snapshot.id = link.criterion_snapshot_id where snapshot.generation_run_id = ? and link.test_case_id = ? and link.coverage_type = 'DIRECT'",
                Integer.class,
                runId,
                testCaseId))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.test_case_revisions where id = ? and change_type is null and change_reason is null",
                Integer.class,
                revisionId))
        .isEqualTo(1);

    UUID rollbackRunId = UUID.randomUUID();
    insertV5GenerationRun(jdbc, rollbackRunId, requirementId, ownerId, "rollback-key");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from testforge.generation_runs where id = ? and provider_adapter_version is null and result_contract_version is null and schema_version is null and validator_version is null and source_requirement_version is null and source_snapshot_provenance is null",
                Integer.class,
                rollbackRunId))
        .isEqualTo(1);
  }

  /** Inserts a generation run using only the columns understood by a V5 binary. */
  private void insertV5GenerationRun(
      JdbcTemplate jdbc, UUID runId, UUID requirementId, UUID ownerId, String keyHash) {
    jdbc.update(
        "insert into testforge.generation_runs (id, requirement_id, requested_by, provider, model, prompt_version, status, input_hash, idempotency_key_hash, started_at, generated_case_count, correlation_id) values (?, ?, ?, 'legacy-provider', 'legacy-model', 'manual-test-v1', 'COMPLETED', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', ?, current_timestamp, 1, 'legacy-correlation')",
        runId,
        requirementId,
        ownerId,
        keyHash);
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
