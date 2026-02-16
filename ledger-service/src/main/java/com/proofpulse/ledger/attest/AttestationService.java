package com.proofpulse.ledger.attest;

import com.proofpulse.ledger.chain.ChainVerificationService;
import com.proofpulse.ledger.crypto.CanonicalJson;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AttestationService {

  private final ChainVerificationService verifier;
  private final KeyManager keys;

  public AttestationService(ChainVerificationService verifier, KeyManager keys) {
    this.verifier = verifier;
    this.keys = keys;
  }

  public Map<String, Object> generate(String projectId, String artifactId, String issuer) throws Exception {
    Map<String, Object> report = verifier.verify(projectId, artifactId);
    if (report == null) return null;
    if (!(Boolean) report.get("valid")) {
      return Map.of(
          "error", "Chain is invalid; cannot attest",
          "verification", report
      );
    }

    // Attestation payload (what we sign)
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("schemaVersion", 1);
    payload.put("type", "EVIDENCE_LEDGER_ATTESTATION");
    payload.put("issuedAt", Instant.now().toString());
    payload.put("issuer", issuer == null ? "proofpulse-ledger-service" : issuer);
    payload.put("projectId", projectId);
    payload.put("artifactId", artifactId);
    payload.put("headChainIndex", report.get("headChainIndex"));
    payload.put("headHash", report.get("headHash"));

    String canonicalPayload = CanonicalJson.stringify(payload);

    // Sign with Ed25519
    Signature sig = Signature.getInstance("Ed25519");
    sig.initSign(keys.privateKey());
    sig.update(canonicalPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String signatureB64 = Base64.getEncoder().encodeToString(sig.sign());

    // Full bundle = payload + signature + pubkey + verification steps
    Map<String, Object> bundle = new LinkedHashMap<>();
    bundle.put("payload", payload);
    bundle.put("payloadCanonical", canonicalPayload);
    bundle.put("signatureB64", signatureB64);
    bundle.put("publicKeyB64", keys.publicKeyBase64());
    bundle.put("algorithm", "Ed25519");
    bundle.put("verificationSteps", new String[]{
        "1) Recompute chain head using /chains/verify for projectId+artifactId.",
        "2) Confirm headHash and headChainIndex match attestation payload.",
        "3) Verify Ed25519 signature over payloadCanonical using publicKeyB64."
    });

    return bundle;
  }
}
