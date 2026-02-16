package com.proofpulse.ledger.attest;

import com.proofpulse.ledger.chain.ChainVerificationService;
import com.proofpulse.ledger.crypto.CanonicalJson;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Service
public class AttestationVerifyService {

  private final ChainVerificationService chainVerifier;

  public AttestationVerifyService(ChainVerificationService chainVerifier) {
    this.chainVerifier = chainVerifier;
  }

  public Map<String, Object> verify(Map<String, Object> bundle) throws Exception {
    // Required fields
    String algorithm = asString(bundle.get("algorithm"));
    String signatureB64 = asString(bundle.get("signatureB64"));
    String publicKeyB64 = asString(bundle.get("publicKeyB64"));

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) bundle.get("payload");

    if (payload == null) {
      return fail("Missing 'payload' object");
    }
    if (!"Ed25519".equalsIgnoreCase(algorithm)) {
      return fail("Unsupported algorithm: " + algorithm);
    }

    // 1) Canonicalize payload deterministically
    String canonicalComputed = CanonicalJson.stringify(payload);

    // Optional: compare to payloadCanonical if present
    String payloadCanonical = bundle.get("payloadCanonical") == null ? null : asString(bundle.get("payloadCanonical"));
    boolean payloadCanonicalMatches = (payloadCanonical == null) || payloadCanonical.equals(canonicalComputed);

    // 2) Verify signature over canonical payload
    PublicKey pub = decodeEd25519PublicKey(publicKeyB64);
    boolean sigOk = verifyEd25519(pub, signatureB64, canonicalComputed);

    // 3) Verify chain head matches payload
    String projectId = asString(payload.get("projectId"));
    String artifactId = asString(payload.get("artifactId"));

    Map<String, Object> chainReport = chainVerifier.verify(projectId, artifactId);
    if (chainReport == null) {
      return failWithDetails("No chain found for project/artifact", Map.of(
          "projectId", projectId,
          "artifactId", artifactId
      ));
    }

    boolean chainValid = Boolean.TRUE.equals(chainReport.get("valid"));
    Object headIndex = chainReport.get("headChainIndex");
    Object headHash = chainReport.get("headHash");

    Object attIndex = payload.get("headChainIndex");
    Object attHash = payload.get("headHash");

    boolean headMatches = Objects.equals(String.valueOf(attIndex), String.valueOf(headIndex))
        && Objects.equals(String.valueOf(attHash), String.valueOf(headHash));

    boolean ok = payloadCanonicalMatches && sigOk && chainValid && headMatches;

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("verifiedAt", Instant.now().toString());
    out.put("ok", ok);

    out.put("signatureValid", sigOk);
    out.put("payloadCanonicalMatches", payloadCanonicalMatches);
    out.put("chainValid", chainValid);
    out.put("chainHeadMatchesAttestation", headMatches);

    out.put("attestationHeadChainIndex", attIndex);
    out.put("attestationHeadHash", attHash);
    out.put("currentHeadChainIndex", headIndex);
    out.put("currentHeadHash", headHash);

    if (!ok) {
      out.put("chainReport", chainReport);
    }

    return out;
  }

  private static boolean verifyEd25519(PublicKey pub, String signatureB64, String message) throws Exception {
    byte[] sigBytes = Base64.getDecoder().decode(signatureB64);
    Signature sig = Signature.getInstance("Ed25519");
    sig.initVerify(pub);
    sig.update(message.getBytes(StandardCharsets.UTF_8));
    return sig.verify(sigBytes);
  }

  private static PublicKey decodeEd25519PublicKey(String publicKeyB64) throws Exception {
    byte[] bytes = Base64.getDecoder().decode(publicKeyB64);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
    return KeyFactory.getInstance("Ed25519").generatePublic(spec);
  }

  private static String asString(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static Map<String, Object> fail(String reason) {
    return Map.of("ok", false, "error", reason);
  }

  private static Map<String, Object> failWithDetails(String reason, Map<String, Object> details) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", false);
    out.put("error", reason);
    out.putAll(details);
    return out;
  }
}
