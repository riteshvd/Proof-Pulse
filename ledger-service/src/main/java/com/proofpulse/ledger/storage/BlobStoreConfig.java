package com.proofpulse.ledger.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStoreConfig {

  @Bean
  public BlobStore blobStore(LocalBlobStore local, S3BlobStore s3) {
    String bucket = System.getenv("PP_S3_BUCKET");
    if (bucket != null && !bucket.isBlank()) {
      return s3;
    }
    return local;
  }
}
