-- Phase 1 upgrade for users who previously ran Phase 0 schema.
-- Safe / idempotent migration.

-- 1) Add new columns if missing
ALTER TABLE evidence_events
  ADD COLUMN IF NOT EXISTS schema_version INT,
  ADD COLUMN IF NOT EXISTS project_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS artifact_id VARCHAR(128);

-- 2) Backfill defaults if null (for old rows)
UPDATE evidence_events
SET
  schema_version = COALESCE(schema_version, 1),
  project_id     = COALESCE(project_id, 'legacy'),
  artifact_id    = COALESCE(artifact_id, 'legacy')
WHERE
  schema_version IS NULL
  OR project_id IS NULL
  OR artifact_id IS NULL;

-- 3) Ensure constraints
ALTER TABLE evidence_events
  ALTER COLUMN schema_version SET NOT NULL,
  ALTER COLUMN project_id SET NOT NULL,
  ALTER COLUMN artifact_id SET NOT NULL;

-- 4) Ensure ts is TIMESTAMPTZ (Phase 0 used TEXT)
DO $$
DECLARE
  col_type TEXT;
BEGIN
  SELECT data_type INTO col_type
  FROM information_schema.columns
  WHERE table_name='evidence_events' AND column_name='ts';

  IF col_type = 'text' THEN
    ALTER TABLE evidence_events
      ALTER COLUMN ts TYPE TIMESTAMPTZ
      USING ts::timestamptz;
  END IF;
END $$;

-- 5) Indexes
CREATE INDEX IF NOT EXISTS idx_evidence_events_project_inserted
  ON evidence_events(project_id, inserted_at DESC);

CREATE INDEX IF NOT EXISTS idx_evidence_events_artifact_inserted
  ON evidence_events(artifact_id, inserted_at DESC);
