package com.proofpulse.ledger.chain;

import com.fasterxml.jackson.databind.JsonNode;
import com.proofpulse.ledger.crypto.EventCanonical;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class ChainVerificationService {

  private final JdbcTemplate jdbc;

  public ChainVerificationService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> verify(String projectId, String artifactId) {
    List<Row> rows = loadRows(projectId, artifactId);
    if (rows.isEmpty()) {
      return result(projectId, artifactId, false, 0, null,
          "no_chain", null, null, null, null);
    }

    String prev = null;

    for (int i = 0; i < rows.size(); i++) {
      Row r = rows.get(i);

      String expectedPrev = (i == 0) ? null : prev;

      if (!Objects.equals(expectedPrev, r.prevHash)) {
        return result(projectId, artifactId, false, rows.size(), (long) i,
            "prev_hash mismatch",
            r.prevHash, expectedPrev,
            r.eventHash, null);
      }

      try {
        JsonNode payloadNode = EventCanonical.mapper().readTree(r.payloadJsonCanonical);

        String canonicalEvent = EventCanonical.canonicalEventJson(
            r.schemaVersion,
            r.eventId,
            r.projectId,
            r.artifactId,
            r.source,
            r.ts,
            r.type,
            payloadNode
        );

        String expectedEventHash = sha256Hex((expectedPrev == null ? "" : expectedPrev) + "|" + canonicalEvent);

        if (!Objects.equals(expectedEventHash, r.eventHash)) {
          return result(projectId, artifactId, false, rows.size(), (long) i,
              "event_hash mismatch",
              r.prevHash, expectedPrev,
              r.eventHash, expectedEventHash);
        }

        prev = expectedEventHash;
      } catch (Exception ex) {
        return result(projectId, artifactId, false, rows.size(), (long) i,
            "verification_error: " + ex.getMessage(),
            null, null, null, null);
      }
    }

    return result(projectId, artifactId, true, rows.size(), null,
        null, null, null, null, null);
  }

  /**
   * Repairs prev_hash + event_hash for all events using the canonical hashing logic.
   * This is the permanent fix once hashing logic changed mid-project.
   */
  @Transactional
  public Map<String, Object> repair(String projectId, String artifactId) {
    List<Row> rows = loadRows(projectId, artifactId);
    if (rows.isEmpty()) {
      return result(projectId, artifactId, false, 0, null,
          "no_chain", null, null, null, null);
    }

    // Prevent concurrent inserts while repairing
    jdbc.update("SELECT pg_advisory_xact_lock(hashtext(?))", projectId + "|" + artifactId);

    String prev = null;

    for (Row r : rows) {
      try {
        JsonNode payloadNode = EventCanonical.mapper().readTree(r.payloadJsonCanonical);

        String canonicalEvent = EventCanonical.canonicalEventJson(
            r.schemaVersion,
            r.eventId,
            r.projectId,
            r.artifactId,
            r.source,
            r.ts,
            r.type,
            payloadNode
        );

        String newEventHash = sha256Hex((prev == null ? "" : prev) + "|" + canonicalEvent);
        String newPrevHash = prev;

        jdbc.update("""
            UPDATE evidence_events
            SET prev_hash = ?, event_hash = ?
            WHERE project_id = ? AND artifact_id = ? AND chain_index = ?
            """,
            newPrevHash,
            newEventHash,
            projectId,
            artifactId,
            r.chainIndex
        );

        prev = newEventHash;
      } catch (Exception ex) {
        return result(projectId, artifactId, false, rows.size(), null,
            "repair_error: " + ex.getMessage(),
            null, null, null, null);
      }
    }

    return verify(projectId, artifactId);
  }

  private List<Row> loadRows(String projectId, String artifactId) {
    return jdbc.query("""
        SELECT event_id, schema_version, project_id, artifact_id, source, ts, type, payload, chain_index, prev_hash, event_hash
        FROM evidence_events
        WHERE project_id=? AND artifact_id=?
        ORDER BY chain_index ASC
        """,
        ps -> { ps.setString(1, projectId); ps.setString(2, artifactId); },
        (rs, i) -> new Row(
            UUID.fromString(rs.getString("event_id")),
            rs.getInt("schema_version"),
            rs.getString("project_id"),
            rs.getString("artifact_id"),
            rs.getString("source"),
            rs.getTimestamp("ts").toInstant(),
            rs.getString("type"),
            rs.getString("payload"),
            rs.getLong("chain_index"),
            rs.getString("prev_hash"),
            rs.getString("event_hash")
        )
    );
  }

  private static Map<String, Object> result(
      String projectId,
      String artifactId,
      boolean valid,
      long totalEvents,
      Long firstMismatchIndex,
      String reason,
      String storedPrevHash,
      String expectedPrevHash,
      String storedEventHash,
      String expectedEventHash
  ) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("projectId", projectId);
    m.put("artifactId", artifactId);
    m.put("computedAt", Instant.now().toString());
    m.put("valid", valid);
    m.put("totalEvents", totalEvents);
    m.put("firstMismatchIndex", firstMismatchIndex);
    m.put("reason", reason);
    m.put("storedPrevHash", storedPrevHash);
    m.put("expectedPrevHash", expectedPrevHash);
    m.put("storedEventHash", storedEventHash);
    m.put("expectedEventHash", expectedEventHash);
    return m;
  }

  private static String sha256Hex(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(dig);
  }

  private record Row(
      UUID eventId,
      int schemaVersion,
      String projectId,
      String artifactId,
      String source,
      Instant ts,
      String type,
      String payloadJsonCanonical,
      long chainIndex,
      String prevHash,
      String eventHash
  ) {}
}
