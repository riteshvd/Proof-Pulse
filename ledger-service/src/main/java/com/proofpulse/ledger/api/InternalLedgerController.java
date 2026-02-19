package com.proofpulse.ledger.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofpulse.ledger.crypto.CanonicalJson;
import com.proofpulse.ledger.crypto.EventCanonical;
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
@RequestMapping("/internal/ledger")
public class InternalLedgerController {

  private final JdbcTemplate jdbc;
  private final ObjectMapper om;

  public InternalLedgerController(JdbcTemplate jdbc, ObjectMapper om) {
    this.jdbc = jdbc;
    this.om = om;
  }

  @PostMapping("/events")
  @Transactional
  public ResponseEntity<?> ingest(@RequestBody String rawJson) {
    try {
      JsonNode n = om.readTree(rawJson);

      int schemaVersion = n.get("schemaVersion").asInt();
      UUID eventId = UUID.fromString(n.get("eventId").asText());
      String projectId = n.get("projectId").asText();
      String artifactId = n.get("artifactId").asText();
      String source = n.get("source").asText();
      String type = n.get("type").asText();
      Instant ts = Instant.parse(n.get("timestamp").asText());

      JsonNode payloadNode = n.has("payload") ? n.get("payload") : EventCanonical.mapper().createObjectNode();

      // ✅ canonical payload string we store in jsonb
      String payloadCanonical = CanonicalJson.canonicalize(payloadNode);

      // ✅ advisory lock (do not use queryForObject)
      String lockKey = projectId + "|" + artifactId;
      jdbc.update("SELECT pg_advisory_xact_lock(hashtext(?))", lockKey);

      Long lastIndex = jdbc.query(
          "SELECT MAX(chain_index) FROM evidence_events WHERE project_id=? AND artifact_id=?",
          ps -> { ps.setString(1, projectId); ps.setString(2, artifactId); },
          rs -> { rs.next(); return (Long) rs.getObject(1); }
      );

      long nextIndex = (lastIndex == null) ? 0L : (lastIndex + 1L);

      String prevHash = null;
      if (nextIndex > 0) {
        prevHash = jdbc.query(
            "SELECT event_hash FROM evidence_events WHERE project_id=? AND artifact_id=? AND chain_index=?",
            ps -> { ps.setString(1, projectId); ps.setString(2, artifactId); ps.setLong(3, nextIndex - 1); },
            rs -> rs.next() ? rs.getString(1) : null
        );
      }

      // ✅ SINGLE SOURCE of canonical event JSON (shared with verifier)
      String canonicalEvent = EventCanonical.canonicalEventJson(
          schemaVersion, eventId, projectId, artifactId, source, ts, type, EventCanonical.mapper().readTree(payloadCanonical)
      );

      String eventHash = sha256Hex((prevHash == null ? "" : prevHash) + "|" + canonicalEvent);

      jdbc.update("""
          INSERT INTO evidence_events
            (event_id, schema_version, project_id, artifact_id, source, ts, type, payload, chain_index, prev_hash, event_hash)
          VALUES
            (?, ?, ?, ?, ?, ?, ?, (?::jsonb), ?, ?, ?)
          """,
          eventId,
          schemaVersion,
          projectId,
          artifactId,
          source,
          Timestamp.from(ts),
          type,
          payloadCanonical,
          nextIndex,
          prevHash,
          eventHash
      );

      return ResponseEntity.ok(Map.of(
          "ok", true,
          "eventId", eventId.toString(),
          "chainIndex", nextIndex,
          "prevHash", prevHash,
          "eventHash", eventHash
      ));
    } catch (Exception e) {
      String msg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
      return ResponseEntity.status(500).body(Map.of("error", "Ledger insert failed", "message", msg));
    }
  }

  private static String sha256Hex(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(dig);
  }
}
