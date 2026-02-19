package com.proofpulse.ledger.storage;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

@Component
public class S3BlobStore implements BlobStore {

  private final String bucket = System.getenv("PP_S3_BUCKET");
  private final String prefix = System.getenv().getOrDefault("PP_S3_PREFIX", "attestations/");
  private final Duration presignTtl =
      Duration.ofMinutes(Long.parseLong(System.getenv().getOrDefault("PP_S3_PRESIGN_MINUTES", "15")));

  private final S3Client s3;
  private final S3Presigner presigner;

  public S3BlobStore() {
    String regionStr = System.getenv().getOrDefault(
        "AWS_REGION",
        System.getenv().getOrDefault("AWS_DEFAULT_REGION", "us-east-1")
    );
    Region region = Region.of(regionStr);

    String endpoint = System.getenv("PP_S3_ENDPOINT"); // optional localstack

    S3ClientBuilder s3b = S3Client.builder()
        .region(region)
        .credentialsProvider(DefaultCredentialsProvider.create());

    S3Presigner.Builder pb = S3Presigner.builder()
        .region(region)
        .credentialsProvider(DefaultCredentialsProvider.create());

    if (endpoint != null && !endpoint.isBlank()) {
      URI uri = URI.create(endpoint);
      s3b = s3b.endpointOverride(uri);
      pb = pb.endpointOverride(uri);
    }

    this.s3 = s3b.build();
    this.presigner = pb.build();
  }

  @Override
  public void put(String key, byte[] data) {
    ensureConfigured();
    String objectKey = prefix + key + ".json";

    s3.putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType("application/json")
            .build(),
        RequestBody.fromBytes(data)
    );
  }

  @Override
  public byte[] get(String key) {
    ensureConfigured();
    String objectKey = prefix + key + ".json";

    ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(
        GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()
    );
    return bytes.asByteArray();
  }

  @Override
  public boolean exists(String key) {
    if (bucket == null || bucket.isBlank()) return false;
    String objectKey = prefix + key + ".json";
    try {
      s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
      return true;
    } catch (S3Exception e) {
      return false;
    }
  }

  public String presignedGetUrl(String key) {
    ensureConfigured();
    String objectKey = prefix + key + ".json";

    GetObjectRequest req = GetObjectRequest.builder()
        .bucket(bucket)
        .key(objectKey)
        .build();

    PresignedGetObjectRequest presigned = presigner.presignGetObject(
        GetObjectPresignRequest.builder()
            .signatureDuration(presignTtl)
            .getObjectRequest(req)
            .build()
    );

    return presigned.url().toString();
  }

  private void ensureConfigured() {
    if (bucket == null || bucket.isBlank()) {
      throw new IllegalStateException("PP_S3_BUCKET not set");
    }
  }
}
