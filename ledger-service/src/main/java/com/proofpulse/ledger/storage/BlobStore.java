package com.proofpulse.ledger.storage;

import java.io.InputStream;
import java.util.Optional;

public interface BlobStore {
  String put(String key, InputStream data, String contentType);
  Optional<InputStream> get(String key);
}
