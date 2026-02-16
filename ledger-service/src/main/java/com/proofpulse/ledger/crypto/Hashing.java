package com.proofpulse.ledger.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class Hashing {
  private Hashing() {}

  public static String sha256Hex(String prevHashOrNull, String canonicalJson) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    String prev = prevHashOrNull == null ? "" : prevHashOrNull;
    digest.update(prev.getBytes(StandardCharsets.UTF_8));
    digest.update(canonicalJson.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest.digest());
  }
}
