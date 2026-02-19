package com.proofpulse.ledger.storage;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalBlobStore implements BlobStore {

  private final Path root = Path.of("attestation-bundles");

  public LocalBlobStore() throws Exception {
    Files.createDirectories(root);
  }

  @Override
  public void put(String key, byte[] data) throws Exception {
    Files.write(root.resolve(key + ".json"), data);
  }

  @Override
  public byte[] get(String key) throws Exception {
    return Files.readAllBytes(root.resolve(key + ".json"));
  }

  @Override
  public boolean exists(String key) {
    return Files.exists(root.resolve(key + ".json"));
  }
}
