CREATE SEQUENCE testforge.work_item_number_seq START WITH 1000 INCREMENT BY 1;

ALTER TABLE requirements ADD COLUMN work_item_number BIGINT;
ALTER TABLE test_cases ADD COLUMN work_item_number BIGINT;

UPDATE requirements
SET work_item_number = nextval('testforge.work_item_number_seq')
WHERE work_item_number IS NULL;

UPDATE test_cases
SET work_item_number = nextval('testforge.work_item_number_seq')
WHERE work_item_number IS NULL;

UPDATE test_cases
SET test_case_key = 'TC-' || CAST(work_item_number AS VARCHAR);

ALTER TABLE requirements ALTER COLUMN work_item_number SET DEFAULT nextval('testforge.work_item_number_seq');
ALTER TABLE requirements ALTER COLUMN work_item_number SET NOT NULL;
ALTER TABLE requirements ADD CONSTRAINT uq_requirements_work_item_number UNIQUE (work_item_number);

ALTER TABLE test_cases ALTER COLUMN work_item_number SET DEFAULT nextval('testforge.work_item_number_seq');
ALTER TABLE test_cases ALTER COLUMN work_item_number SET NOT NULL;
ALTER TABLE test_cases ADD CONSTRAINT uq_test_cases_work_item_number UNIQUE (work_item_number);
ALTER TABLE test_cases ADD CONSTRAINT uq_test_cases_key UNIQUE (test_case_key);
