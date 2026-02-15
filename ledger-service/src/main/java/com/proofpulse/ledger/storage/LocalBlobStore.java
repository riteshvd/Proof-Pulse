package com.proofpulse.ledger.storage;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

@Component
public class LocalBlobStore implements BlobStore {
  @Override
  public String put(String key, InputStream data, String contentType) {
    // Phase 4 will implement S3 + presigned URLs.
    // For now this exists so later cloud integration doesn’t require refactors.
    return key;
  }

  @Override
  public Optional<InputStream> get(String key) {
    return Optional.empty();
  }
}
