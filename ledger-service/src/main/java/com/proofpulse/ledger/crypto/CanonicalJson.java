package com.proofpulse.ledger.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Deterministic JSON canonicalization utilities.
 *
 * Fixes your EC2 /chains/verify Instant error by ensuring the ObjectMapper
 * registers jackson-datatype-jsr310 (and any other modules) via findAndRegisterModules().
 */
public final class CanonicalJson {

  // ✅ This is the important line: register JSR310 automatically
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .findAndRegisterModules()
      .disable(SerializationFeature.INDENT_OUTPUT)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private CanonicalJson() {}

  /** Canonicalize any JsonNode into a stable JSON string (sorted keys, compact). */
  public static String canonicalize(JsonNode node) {
    if (node == null || node.isNull()) return "null";
    JsonNode normalized = normalize(node);
    try {
      return MAPPER.writeValueAsString(normalized);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to canonicalize JSON", e);
    }
  }

  /** Legacy API used by AttestationService + AttestationVerifyService. */
  public static String stringify(Map<String, Object> map) {
    JsonNode node = MAPPER.valueToTree(map == null ? Map.of() : map);
    return canonicalize(node);
  }

  /**
   * Legacy API used by ChainVerificationService: stringify(EvidenceEvent).
   * Keep it generic (Object) so we don't create extra coupling/import issues.
   */
  public static String stringify(Object anyPojo) {
    JsonNode node = MAPPER.valueToTree(anyPojo);
    return canonicalize(node);
  }

  /** Normalize JsonNode (sort object keys recursively). */
  public static JsonNode normalize(JsonNode node) {
    if (node == null || node.isNull()) return NullNode.getInstance();

    if (node.isObject()) {
      ObjectNode out = MAPPER.createObjectNode();
      List<String> keys = new ArrayList<>();
      Iterator<String> it = node.fieldNames();
      while (it.hasNext()) keys.add(it.next());
      keys.sort(Comparator.naturalOrder());
      for (String k : keys) out.set(k, normalize(node.get(k)));
      return out;
    }

    if (node.isArray()) {
      ArrayNode out = MAPPER.createArrayNode();
      for (JsonNode el : node) out.add(normalize(el));
      return out;
    }

    if (node.isNumber()) {
      BigDecimal bd = node.decimalValue().stripTrailingZeros();
      if (bd.compareTo(BigDecimal.ZERO) == 0) bd = BigDecimal.ZERO; // avoid "-0"
      return new DecimalNode(bd);
    }

    if (node.isTextual()) return new TextNode(node.textValue());
    if (node.isBoolean()) return BooleanNode.valueOf(node.booleanValue());

    return new TextNode(node.asText());
  }

  /** Expose the configured mapper if any service needs it. */
  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
