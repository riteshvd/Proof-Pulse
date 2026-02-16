ALTER TABLE evidence_events
  ADD COLUMN IF NOT EXISTS chain_index BIGINT,
  ADD COLUMN IF NOT EXISTS prev_hash VARCHAR(64),
  ADD COLUMN IF NOT EXISTS event_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS ux_evidence_chain_position
  ON evidence_events(project_id, artifact_id, chain_index);
