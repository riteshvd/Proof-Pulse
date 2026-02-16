package com.proofpulse.ledger.chain;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ChainVerificationController {

  private final ChainVerificationService verifier;

  public ChainVerificationController(ChainVerificationService verifier) {
    this.verifier = verifier;
  }

  @GetMapping("/chains/verify")
  public ResponseEntity<?> verify(
      @RequestParam String projectId,
      @RequestParam String artifactId
  ) {
    try {
      Map<String, Object> report = verifier.verify(projectId, artifactId);
      if (report == null) return ResponseEntity.status(404).body(Map.of("error", "No chain found"));
      return ResponseEntity.ok(report);
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of("error", "Verification failed", "message", ex.getMessage()));
    }
  }

  // One-time backfill after schema upgrades
  @PostMapping("/chains/repair")
  public ResponseEntity<?> repair(
      @RequestParam String projectId,
      @RequestParam String artifactId
  ) {
    try {
      Map<String, Object> report = verifier.repair(projectId, artifactId);
      if (report == null) return ResponseEntity.status(404).body(Map.of("error", "No chain found"));
      return ResponseEntity.ok(report);
    } catch (Exception ex) {
      return ResponseEntity.status(500).body(Map.of("error", "Repair failed", "message", ex.getMessage()));
    }
  }
}
