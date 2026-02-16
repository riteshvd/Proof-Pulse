package com.proofpulse.ledger.attest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AttestationController {

  private final AttestationService attest;

  public AttestationController(AttestationService attest) {
    this.attest = attest;
  }

  @PostMapping("/attestations/generate")
  public ResponseEntity<?> generate(
      @RequestParam String projectId,
      @RequestParam String artifactId,
      @RequestParam(required = false) String issuer
  ) {
    try {
      Map<String, Object> bundle = attest.generate(projectId, artifactId, issuer);
      if (bundle == null) return ResponseEntity.status(404).body(Map.of("error", "No chain found"));
      if (bundle.containsKey("error")) return ResponseEntity.status(409).body(bundle); // invalid chain
      return ResponseEntity.ok(bundle);
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of(
          "error", "Attestation generation failed",
          "message", ex.getMessage()
      ));
    }
  }
}
