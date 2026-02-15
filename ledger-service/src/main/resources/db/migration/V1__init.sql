CREATE TABLE IF NOT EXISTS evidence_events (
  event_id UUID PRIMARY KEY,
  schema_version INT NOT NULL,
  project_id VARCHAR(64) NOT NULL,
  artifact_id VARCHAR(128) NOT NULL,

  source TEXT NOT NULL,
  ts TIMESTAMPTZ NOT NULL,
  type TEXT NOT NULL,
  payload JSONB NOT NULL,

  inserted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evidence_events_project_inserted
  ON evidence_events(project_id, inserted_at DESC);

CREATE INDEX IF NOT EXISTS idx_evidence_events_artifact_inserted
  ON evidence_events(artifact_id, inserted_at DESC);
