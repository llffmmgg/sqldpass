ALTER TABLE generation_lock ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IDLE';
UPDATE generation_lock SET status = CASE WHEN running = TRUE THEN 'RUNNING' ELSE 'IDLE' END;
ALTER TABLE generation_lock DROP COLUMN running;
