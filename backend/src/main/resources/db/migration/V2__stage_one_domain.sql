CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE refresh_token_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_id UUID,
    reuse_detected BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_token_user ON refresh_token_sessions(user_id);
CREATE INDEX idx_refresh_token_family ON refresh_token_sessions(family_id);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_projects_owner_updated ON projects(owner_id, updated_at);

CREATE TABLE requirements (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    title VARCHAR(200) NOT NULL,
    user_story VARCHAR(10000) NOT NULL,
    business_requirements VARCHAR(20000) NOT NULL DEFAULT '',
    assumptions VARCHAR(10000) NOT NULL DEFAULT '',
    source_reference VARCHAR(1000) NOT NULL DEFAULT '',
    status VARCHAR(40) NOT NULL CHECK (status IN ('DRAFT', 'READY_FOR_GENERATION', 'GENERATED', 'NEEDS_CLARIFICATION', 'ARCHIVED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_requirements_project_updated ON requirements(project_id, updated_at);

CREATE TABLE acceptance_criteria (
    id UUID PRIMARY KEY,
    requirement_id UUID NOT NULL REFERENCES requirements(id) ON DELETE CASCADE,
    criterion_key VARCHAR(20) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (requirement_id, criterion_key),
    UNIQUE (requirement_id, sort_order)
);

CREATE TABLE requirement_ambiguities (
    id UUID PRIMARY KEY,
    requirement_id UUID NOT NULL REFERENCES requirements(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    suggested_question VARCHAR(4000) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolution VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE generation_runs (
    id UUID PRIMARY KEY,
    requirement_id UUID NOT NULL REFERENCES requirements(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(200) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REJECTED_BY_VALIDATION')),
    input_hash VARCHAR(64) NOT NULL,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    latency_ms BIGINT,
    generated_case_count INTEGER NOT NULL DEFAULT 0,
    input_tokens INTEGER,
    output_tokens INTEGER,
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),
    correlation_id VARCHAR(100) NOT NULL,
    UNIQUE (requirement_id, requested_by, idempotency_key_hash)
);

CREATE INDEX idx_generation_runs_requirement ON generation_runs(requirement_id, started_at);

CREATE TABLE test_cases (
    id UUID PRIMARY KEY,
    requirement_id UUID NOT NULL REFERENCES requirements(id),
    generation_run_id UUID NOT NULL REFERENCES generation_runs(id),
    test_case_key VARCHAR(30) NOT NULL,
    title VARCHAR(300) NOT NULL,
    objective VARCHAR(4000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    automation_candidate BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('GENERATED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'NEEDS_REVISION')),
    coverage_intent VARCHAR(40) NOT NULL CHECK (coverage_intent IN ('ACCEPTANCE_CRITERIA', 'SUPPORTING_EXPLORATORY')),
    rationale VARCHAR(4000) NOT NULL,
    final_expected_outcome VARCHAR(4000) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (requirement_id, test_case_key)
);

CREATE INDEX idx_test_cases_requirement ON test_cases(requirement_id, status);

CREATE TABLE test_case_preconditions (
    id UUID PRIMARY KEY,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    description VARCHAR(4000) NOT NULL,
    UNIQUE (test_case_id, sort_order)
);

CREATE TABLE test_steps (
    id UUID PRIMARY KEY,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL CHECK (step_number > 0),
    action VARCHAR(4000) NOT NULL,
    expected_result VARCHAR(4000) NOT NULL,
    test_data_reference VARCHAR(1000),
    UNIQUE (test_case_id, step_number)
);

CREATE TABLE test_data_items (
    id UUID PRIMARY KEY,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    example_value VARCHAR(1000) NOT NULL,
    sensitivity VARCHAR(20) NOT NULL CHECK (sensitivity IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED')),
    generation_strategy VARCHAR(100) NOT NULL,
    UNIQUE (test_case_id, name)
);

CREATE TABLE traceability_links (
    id UUID PRIMARY KEY,
    acceptance_criterion_id UUID NOT NULL REFERENCES acceptance_criteria(id) ON DELETE CASCADE,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    coverage_type VARCHAR(20) NOT NULL CHECK (coverage_type IN ('DIRECT', 'PARTIAL', 'SUPPORTING')),
    confidence DECIMAL(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (acceptance_criterion_id, test_case_id)
);

CREATE TABLE test_case_reviews (
    id UUID PRIMARY KEY,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id),
    decision VARCHAR(30) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED', 'CHANGES_REQUESTED')),
    comments VARCHAR(4000) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE requirement_revisions (
    id UUID PRIMARY KEY,
    requirement_id UUID NOT NULL REFERENCES requirements(id) ON DELETE CASCADE,
    revision_number BIGINT NOT NULL,
    snapshot_json TEXT NOT NULL,
    changed_by UUID NOT NULL REFERENCES users(id),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (requirement_id, revision_number)
);

CREATE TABLE test_case_revisions (
    id UUID PRIMARY KEY,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    revision_number BIGINT NOT NULL,
    snapshot_json TEXT NOT NULL,
    changed_by UUID NOT NULL REFERENCES users(id),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (test_case_id, revision_number)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    project_id UUID REFERENCES projects(id),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(100) NOT NULL,
    metadata TEXT NOT NULL DEFAULT '{}',
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(100) NOT NULL
);

CREATE INDEX idx_audit_project_timestamp ON audit_events(project_id, event_timestamp);
