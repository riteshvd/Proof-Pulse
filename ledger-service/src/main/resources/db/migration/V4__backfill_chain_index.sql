-- Backfill chain_index for pre-hash-chain rows (if any).
-- Ensures a deterministic order using inserted_at then event_id.

WITH ranked AS (
  SELECT
    event_id,
    ROW_NUMBER() OVER (
      PARTITION BY project_id, artifact_id
      ORDER BY inserted_at ASC, event_id ASC
    ) - 1 AS rn
  FROM evidence_events
  WHERE chain_index IS NULL
)
UPDATE evidence_events e
SET chain_index = r.rn
FROM ranked r
WHERE e.event_id = r.event_id;
