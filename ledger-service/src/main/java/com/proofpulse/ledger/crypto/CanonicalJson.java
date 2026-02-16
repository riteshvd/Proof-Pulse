package com.proofpulse.ledger.crypto;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class CanonicalJson {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule())                // Support Instant
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  private CanonicalJson() {}

  public static String stringify(Object value) throws Exception {
    return MAPPER.writeValueAsString(value);
  }
}
