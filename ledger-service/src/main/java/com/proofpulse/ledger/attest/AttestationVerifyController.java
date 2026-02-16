package com.proofpulse.ledger.attest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AttestationVerifyController {

  private final AttestationVerifyService verifier;

  public AttestationVerifyController(AttestationVerifyService verifier) {
    this.verifier = verifier;
  }

  @PostMapping("/attestations/verify")
  public ResponseEntity<?> verify(@RequestBody Map<String, Object> bundle) {
    try {
      Map<String, Object> result = verifier.verify(bundle);
      boolean ok = Boolean.TRUE.equals(result.get("ok"));
      return ok ? ResponseEntity.ok(result) : ResponseEntity.status(409).body(result);
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of(
          "ok", false,
          "error", "Attestation verification failed",
          "message", ex.getMessage()
      ));
    }
  }
}
