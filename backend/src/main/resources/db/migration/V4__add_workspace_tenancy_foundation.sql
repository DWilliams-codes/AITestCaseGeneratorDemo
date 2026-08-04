CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_workspaces_creator_status_updated
    ON workspaces(created_by, status, updated_at);

CREATE TABLE workspace_memberships (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'QA_AUTHOR', 'QA_REVIEWER', 'STAKEHOLDER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_workspace_membership UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_memberships_user_workspace
    ON workspace_memberships(user_id, workspace_id);
CREATE INDEX idx_workspace_memberships_workspace_role
    ON workspace_memberships(workspace_id, role);

ALTER TABLE projects ADD COLUMN workspace_id UUID;

INSERT INTO workspaces (id, name, status, created_by, created_at, updated_at, version)
SELECT id, display_name || '''s Workspace', 'ACTIVE', id, created_at, updated_at, 0
FROM users;

INSERT INTO workspace_memberships (
    id, workspace_id, user_id, role, status, created_by, created_at, updated_at, version
)
SELECT id, id, id, 'OWNER', 'ACTIVE', id, created_at, updated_at, 0
FROM users;

UPDATE projects
SET workspace_id = owner_id
WHERE workspace_id IS NULL;

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

CREATE INDEX idx_projects_workspace_updated ON projects(workspace_id, updated_at);
