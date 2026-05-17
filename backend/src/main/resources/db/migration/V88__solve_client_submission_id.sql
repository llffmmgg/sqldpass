ALTER TABLE solve
  ADD COLUMN client_submission_id VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_solve_member_client_submission
  ON solve (member_id, client_submission_id);
