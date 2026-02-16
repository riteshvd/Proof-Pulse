package com.proofpulse.ledger.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofpulse.ledger.crypto.CanonicalJson;
import com.proofpulse.ledger.crypto.Hashing;
import com.proofpulse.ledger.model.EvidenceEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ChainVerificationService {

  private final JdbcTemplate jdbc;
  private final ObjectMapper parseMapper = new ObjectMapper();

  public ChainVerificationService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> verify(String projectId, String artifactId) throws Exception {
    List<Row> rows = load(projectId, artifactId);
    if (rows.isEmpty()) return null;

    String computedPrev = null;
    long headIndex = -1;
    String headHash = null;

    for (Row r : rows) {
      if (!Objects.equals(r.prevHash, computedPrev)) {
        return mismatch(projectId, artifactId, rows.size(), r.chainIndex,
            "prev_hash mismatch", r.prevHash, computedPrev, r.eventHash, null);
      }

      EvidenceEvent e = toEvent(r);
      String canonical = CanonicalJson.stringify(e);
      String expectedHash = Hashing.sha256Hex(computedPrev, canonical);

      if (!Objects.equals(r.eventHash, expectedHash)) {
        return mismatch(projectId, artifactId, rows.size(), r.chainIndex,
            "event_hash mismatch", r.prevHash, computedPrev, r.eventHash, expectedHash);
      }

      computedPrev = r.eventHash;
      headIndex = r.chainIndex;
      headHash = r.eventHash;
    }

    return Map.of(
      "projectId", projectId,
      "artifactId", artifactId,
      "computedAt", Instant.now().toString(),
      "valid", true,
      "totalEvents", rows.size(),
      "headChainIndex", headIndex,
      "headHash", headHash
    );
  }

  @Transactional
  public Map<String, Object> repair(String projectId, String artifactId) throws Exception {
    List<Row> rows = load(projectId, artifactId);
    if (rows.isEmpty()) return null;

    String computedPrev = null;

    for (Row r : rows) {
      EvidenceEvent e = toEvent(r);
      String canonical = CanonicalJson.stringify(e);
      String expectedHash = Hashing.sha256Hex(computedPrev, canonical);

      // update row hashes (even if already present; safe)
      jdbc.update("""
          UPDATE evidence_events
          SET prev_hash = ?, event_hash = ?
          WHERE event_id = ?
        """, computedPrev, expectedHash, r.eventId);

      computedPrev = expectedHash;
    }

    // return fresh verification
    return verify(projectId, artifactId);
  }

  private List<Row> load(String projectId, String artifactId) {
    return jdbc.query("""
        SELECT event_id, schema_version, project_id, artifact_id, source, ts, type, payload,
               chain_index, prev_hash, event_hash
        FROM evidence_events
        WHERE project_id = ? AND artifact_id = ?
        ORDER BY chain_index ASC
      """, (rs, i) -> {
      Row r = new Row();
      r.eventId = UUID.fromString(rs.getString("event_id"));
      r.schemaVersion = rs.getInt("schema_version");
      r.projectId = rs.getString("project_id");
      r.artifactId = rs.getString("artifact_id");
      r.source = rs.getString("source");

      OffsetDateTime odt = rs.getObject("ts", OffsetDateTime.class);
      r.timestamp = odt.toInstant();

      r.type = rs.getString("type");
      Object payloadObj = rs.getObject("payload");
      r.payloadJson = (payloadObj == null) ? "{}" : payloadObj.toString();

      r.chainIndex = rs.getLong("chain_index");
      r.prevHash = rs.getString("prev_hash");
      r.eventHash = rs.getString("event_hash");
      return r;
    }, projectId, artifactId);
  }

  private EvidenceEvent toEvent(Row r) throws Exception {
    EvidenceEvent e = new EvidenceEvent();
    e.schemaVersion = r.schemaVersion;
    e.eventId = r.eventId;
    e.projectId = r.projectId;
    e.artifactId = r.artifactId;
    e.source = r.source;
    e.timestamp = r.timestamp;
    e.type = r.type;

    @SuppressWarnings("unchecked")
    Map<String, Object> payloadMap = parseMapper.readValue(r.payloadJson, Map.class);
    e.payload = payloadMap;

    return e;
  }

  private Map<String, Object> mismatch(
      String projectId,
      String artifactId,
      int total,
      long firstBadIndex,
      String reason,
      String storedPrev,
      String expectedPrev,
      String storedHash,
      String expectedHash
  ) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("projectId", projectId);
    out.put("artifactId", artifactId);
    out.put("computedAt", Instant.now().toString());
    out.put("valid", false);
    out.put("totalEvents", total);
    out.put("firstMismatchIndex", firstBadIndex);
    out.put("reason", reason);
    out.put("storedPrevHash", storedPrev);
    out.put("expectedPrevHash", expectedPrev);
    out.put("storedEventHash", storedHash);
    out.put("expectedEventHash", expectedHash);
    return out;
  }

  private static class Row {
    UUID eventId;
    int schemaVersion;
    String projectId;
    String artifactId;
    String source;
    Instant timestamp;
    String type;
    String payloadJson;
    long chainIndex;
    String prevHash;
    String eventHash;
  }
}
