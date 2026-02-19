package com.proofpulse.ledger.storage;

public interface BlobStore {
  void put(String key, byte[] data) throws Exception;
  byte[] get(String key) throws Exception;
  boolean exists(String key);
}
