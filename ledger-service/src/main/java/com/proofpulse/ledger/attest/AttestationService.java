package com.proofpulse.ledger.attest;

import com.proofpulse.ledger.chain.ChainVerificationService;
import com.proofpulse.ledger.crypto.CanonicalJson;
import com.proofpulse.ledger.storage.BlobStore;
import com.proofpulse.ledger.storage.S3BlobStore;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AttestationService {

  private final ChainVerificationService verifier;
  private final KeyManager keys;
  private final BlobStore blobStore;

  public AttestationService(ChainVerificationService verifier, KeyManager keys, BlobStore blobStore) {
    this.verifier = verifier;
    this.keys = keys;
    this.blobStore = blobStore;
  }

  public Map<String, Object> generateAndStore(String projectId, String artifactId, String issuer) throws Exception {
    Map<String, Object> report = verifier.verify(projectId, artifactId);
    if (report == null) return null;
    if (!Boolean.TRUE.equals(report.get("valid"))) {
      return Map.of("error", "Chain is invalid", "verification", report);
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("schemaVersion", 1);
    payload.put("type", "EVIDENCE_LEDGER_ATTESTATION");
    payload.put("issuedAt", Instant.now().toString());
    payload.put("issuer", issuer == null ? "proofpulse-ledger-service" : issuer);
    payload.put("projectId", projectId);
    payload.put("artifactId", artifactId);
    payload.put("headChainIndex", report.get("headChainIndex"));
    payload.put("headHash", report.get("headHash"));

    String canonical = CanonicalJson.stringify(payload);

    Signature sig = Signature.getInstance("Ed25519");
    sig.initSign(keys.privateKey());
    sig.update(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String signatureB64 = Base64.getEncoder().encodeToString(sig.sign());

    Map<String, Object> bundle = new LinkedHashMap<>();
    bundle.put("payload", payload);
    bundle.put("payloadCanonical", canonical);
    bundle.put("signatureB64", signatureB64);
    bundle.put("publicKeyB64", keys.publicKeyBase64());
    bundle.put("algorithm", "Ed25519");
    bundle.put("verificationSteps", new String[]{
        "1) Recompute chain head using /chains/verify?projectId=...&artifactId=...",
        "2) Confirm headHash and headChainIndex match attestation payload.",
        "3) Verify Ed25519 signature over payloadCanonical using publicKeyB64."
    });

    String bundleId = UUID.randomUUID().toString();
    byte[] bytes = CanonicalJson.stringify(bundle).getBytes(java.nio.charset.StandardCharsets.UTF_8);

    blobStore.put(bundleId, bytes);

    if (blobStore instanceof S3BlobStore s3) {
      return Map.of(
          "bundleId", bundleId,
          "downloadUrl", s3.presignedGetUrl(bundleId),
          "expiresInMinutes", Long.parseLong(System.getenv().getOrDefault("PP_S3_PRESIGN_MINUTES", "15"))
      );
    }

    return Map.of("bundleId", bundleId, "downloadEndpoint", "/attestations/" + bundleId);
  }
}
