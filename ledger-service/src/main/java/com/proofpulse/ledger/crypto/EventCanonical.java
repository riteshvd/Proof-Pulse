package com.proofpulse.ledger.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Single source of truth for the canonical event JSON used for hashing.
 * BOTH ingest and verify MUST call this.
 */
public final class EventCanonical {

  private static final ObjectMapper OM = new ObjectMapper()
      .findAndRegisterModules()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(SerializationFeature.INDENT_OUTPUT);

  private EventCanonical() {}

  public static String canonicalEventJson(
      int schemaVersion,
      UUID eventId,
      String projectId,
      String artifactId,
      String source,
      Instant timestamp,
      String type,
      JsonNode payloadNode
  ) {
    ObjectNode root = OM.createObjectNode();
    root.put("schemaVersion", schemaVersion);
    root.put("eventId", eventId.toString());
    root.put("projectId", projectId);
    root.put("artifactId", artifactId);
    root.put("source", source);
    root.put("timestamp", timestamp.toString());
    root.put("type", type);

    // payload must be a JSON node (object/array/etc). If missing, use empty object.
    if (payloadNode == null || payloadNode.isNull()) {
      root.set("payload", OM.createObjectNode());
    } else {
      root.set("payload", payloadNode);
    }

    // ✅ canonicalize sorts keys recursively + compact
    return CanonicalJson.canonicalize(root);
  }

  public static ObjectMapper mapper() {
    return OM;
  }
}
