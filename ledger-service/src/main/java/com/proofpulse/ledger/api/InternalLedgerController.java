package com.proofpulse.ledger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.proofpulse.ledger.model.EvidenceEvent;
import jakarta.validation.Valid;
import org.postgresql.util.PGobject;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@RestController
public class InternalLedgerController {

  private final JdbcTemplate jdbc;
  private final ObjectMapper om;

  public InternalLedgerController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    this.om = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  @Transactional
  @PostMapping("/internal/ledger/events")
  public ResponseEntity<?> append(@Valid @RequestBody EvidenceEvent e) {

    if (e.schemaVersion == null || e.schemaVersion != 1) {
      return ResponseEntity.badRequest().body(Map.of("error", "Unsupported schemaVersion"));
    }

    try {
      var prev = jdbc.query("""
          SELECT chain_index, event_hash
          FROM evidence_events
          WHERE project_id = ? AND artifact_id = ?
          ORDER BY chain_index DESC
          LIMIT 1
        """,
        rs -> {
          if (!rs.next()) return null;
          return Map.of(
            "chainIndex", rs.getLong("chain_index"),
            "eventHash", rs.getString("event_hash")
          );
        },
        e.projectId, e.artifactId
      );

      long nextIndex = 0;
      String prevHash = null;

      if (prev != null) {
        nextIndex = ((Long) prev.get("chainIndex")) + 1;
        prevHash = (String) prev.get("eventHash");
      }

      String canonical = om.writeValueAsString(e);

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update((prevHash == null ? "" : prevHash).getBytes(StandardCharsets.UTF_8));
      digest.update(canonical.getBytes(StandardCharsets.UTF_8));
      String eventHash = HexFormat.of().formatHex(digest.digest());

      PGobject jsonb = new PGobject();
      jsonb.setType("jsonb");
      jsonb.setValue(om.writeValueAsString(e.payload));

      Instant ts = e.timestamp != null ? e.timestamp : Instant.now();

      jdbc.update("""
          INSERT INTO evidence_events
            (event_id, schema_version, project_id, artifact_id,
             source, ts, type, payload,
             chain_index, prev_hash, event_hash)
          VALUES
            (?,?,?,?,?,?,?,?,?,?,?)
        """,
        e.eventId,
        e.schemaVersion,
        e.projectId,
        e.artifactId,
        e.source,
        Timestamp.from(ts),
        e.type,
        jsonb,
        nextIndex,
        prevHash,
        eventHash
      );

      return ResponseEntity.status(201).body(Map.of(
        "eventId", e.eventId,
        "chainIndex", nextIndex,
        "eventHash", eventHash,
        "status", "STORED"
      ));

    } catch (DuplicateKeyException dke) {
      return ResponseEntity.status(409).body(Map.of("error", "Duplicate eventId"));
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of(
        "error", "Ledger insert failed",
        "message", ex.getMessage()
      ));
    }
  }

  // Chain head using query params (no path parsing issues)
  @GetMapping("/chains/head")
  public ResponseEntity<?> head(
      @RequestParam String projectId,
      @RequestParam String artifactId
  ) {
    var row = jdbc.query("""
        SELECT chain_index, event_hash
        FROM evidence_events
        WHERE project_id = ? AND artifact_id = ?
        ORDER BY chain_index DESC
        LIMIT 1
      """,
      rs -> {
        if (!rs.next()) return null;
        return Map.of(
          "chainIndex", rs.getLong("chain_index"),
          "eventHash", rs.getString("event_hash")
        );
      },
      projectId, artifactId
    );

    if (row == null) {
      return ResponseEntity.status(404).body(Map.of("error", "No chain found"));
    }

    return ResponseEntity.ok(row);
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
