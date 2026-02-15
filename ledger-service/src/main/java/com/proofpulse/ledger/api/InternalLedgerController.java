package com.proofpulse.ledger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofpulse.ledger.model.EvidenceEvent;
import jakarta.validation.Valid;
import org.postgresql.util.PGobject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
public class InternalLedgerController {

  private final JdbcTemplate jdbc;
  private final ObjectMapper om;

  public InternalLedgerController(JdbcTemplate jdbc, ObjectMapper om) {
    this.jdbc = jdbc;
    this.om = om;
  }

  @PostMapping("/internal/ledger/events")
  public ResponseEntity<?> append(@Valid @RequestBody EvidenceEvent e) {
    if (e.schemaVersion == null || e.schemaVersion != 1) {
      return ResponseEntity.badRequest().body(Map.of("error", "Unsupported schemaVersion (expected 1)"));
    }

    try {
      // Bind JSONB properly (avoids flaky ?::jsonb string casting)
      PGobject jsonb = new PGobject();
      jsonb.setType("jsonb");
      jsonb.setValue(om.writeValueAsString(e.payload));

      Instant ts = e.timestamp != null ? e.timestamp : Instant.now();

      jdbc.update("""
          INSERT INTO evidence_events
            (event_id, schema_version, project_id, artifact_id, source, ts, type, payload)
          VALUES
            (?,?,?,?,?,?,?,?)
        """,
        e.eventId,
        e.schemaVersion,
        e.projectId,
        e.artifactId,
        e.source,
        Timestamp.from(ts),
        e.type,
        jsonb
      );

      return ResponseEntity.status(201).body(Map.of("eventId", e.eventId, "status", "STORED"));
    } catch (DuplicateKeyException dke) {
      return ResponseEntity.status(409).body(Map.of("error", "Duplicate eventId"));
    } catch (Exception ex) {
      // Dev-friendly error response so you can see the real cause
      return ResponseEntity.status(500).body(Map.of(
        "error", "Ledger insert failed",
        "message", ex.getMessage()
      ));
    }
  }

  @GetMapping("/events/{eventId}")
  public ResponseEntity<?> get(@PathVariable("eventId") UUID eventId) {
    var row = jdbc.query("""
        SELECT event_id, schema_version, project_id, artifact_id, source, ts, type, payload, inserted_at
        FROM evidence_events
        WHERE event_id = ?
      """,
      rs -> {
        if (!rs.next()) return null;
        return Map.of(
          "schemaVersion", rs.getInt("schema_version"),
          "eventId", rs.getString("event_id"),
          "projectId", rs.getString("project_id"),
          "artifactId", rs.getString("artifact_id"),
          "source", rs.getString("source"),
          "timestamp", rs.getString("ts"),
          "type", rs.getString("type"),
          "payload", rs.getObject("payload").toString(),
          "insertedAt", rs.getString("inserted_at")
        );
      },
      eventId
    );

    if (row == null) return ResponseEntity.status(404).body(Map.of("error", "Not found"));
    return ResponseEntity.ok(row);
  }
}
