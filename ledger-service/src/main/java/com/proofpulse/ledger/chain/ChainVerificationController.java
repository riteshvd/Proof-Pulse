package com.proofpulse.ledger.chain;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chains")
public class ChainVerificationController {

  private final ChainVerificationService verifier;

  public ChainVerificationController(ChainVerificationService verifier) {
    this.verifier = verifier;
  }

  @GetMapping("/verify")
  public ResponseEntity<Map<String, Object>> verify(
      @RequestParam String projectId,
      @RequestParam String artifactId
  ) {
    return ResponseEntity.ok(verifier.verify(projectId, artifactId));
  }

  @PostMapping("/repair")
  public ResponseEntity<Map<String, Object>> repair(
      @RequestParam String projectId,
      @RequestParam String artifactId
  ) {
    return ResponseEntity.ok(verifier.repair(projectId, artifactId));
  }
}
