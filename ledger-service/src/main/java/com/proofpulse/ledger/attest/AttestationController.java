package com.proofpulse.ledger.attest;

import com.proofpulse.ledger.storage.BlobStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class AttestationController {

  private final AttestationService attest;
  private final BlobStore blobStore;

  public AttestationController(AttestationService attest, BlobStore blobStore) {
    this.attest = attest;
    this.blobStore = blobStore;
  }

  @PostMapping("/attestations/generate")
  public ResponseEntity<?> generate(
      @RequestParam String projectId,
      @RequestParam String artifactId,
      @RequestParam(required = false) String issuer
  ) {
    try {
      Map<String, Object> result = attest.generateAndStore(projectId, artifactId, issuer);
      if (result == null) return ResponseEntity.status(404).body(Map.of("error", "No chain found"));
      if (result.containsKey("error")) return ResponseEntity.status(409).body(result);
      return ResponseEntity.ok(result);
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of(
          "error", "Attestation generation failed",
          "message", ex.getMessage()
      ));
    }
  }

  @GetMapping("/attestations/{bundleId}")
  public ResponseEntity<?> download(@PathVariable String bundleId) {
    try {
      if (!blobStore.exists(bundleId)) {
        return ResponseEntity.status(404).body(Map.of("error", "Bundle not found"));
      }
      byte[] data = blobStore.get(bundleId);
      return ResponseEntity.ok()
          .header("Content-Type", "application/json")
          .body(new String(data, StandardCharsets.UTF_8));
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of(
          "error", "Download failed",
          "message", ex.getMessage()
      ));
    }
  }
}
