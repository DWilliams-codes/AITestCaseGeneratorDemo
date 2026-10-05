CREATE TABLE test_case_setup_steps (
  id UUID PRIMARY KEY,
  test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
  step_number INTEGER NOT NULL CHECK (step_number > 0),
  action VARCHAR(4000) NOT NULL,
  expected_result VARCHAR(4000) NOT NULL,
  test_data_reference VARCHAR(1000),
  CONSTRAINT uk_test_case_setup_steps_case_number UNIQUE (test_case_id, step_number)
);
