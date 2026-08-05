ALTER TABLE requirements ADD COLUMN priority VARCHAR(20);

ALTER TABLE requirements
  ADD CONSTRAINT ck_requirements_priority
  CHECK (priority IS NULL OR priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW'));

UPDATE requirements SET priority = 'MEDIUM' WHERE priority IS NULL;
