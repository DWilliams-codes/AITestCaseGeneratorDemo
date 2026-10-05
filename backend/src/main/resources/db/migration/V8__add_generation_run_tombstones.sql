ALTER TABLE generation_runs
  ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE generation_runs
  ADD COLUMN deleted_by UUID REFERENCES users(id);

ALTER TABLE generation_runs
  ADD CONSTRAINT ck_generation_runs_deletion_pair
  CHECK ((deleted_at IS NULL) = (deleted_by IS NULL));

CREATE INDEX ix_generation_runs_visible_history
  ON generation_runs(requirement_id, deleted_at, started_at DESC);
