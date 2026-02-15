package com.proofpulse.ledger.model;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class EvidenceEvent {

  @NotNull
  public Integer schemaVersion; // must be 1 for Phase 1

  @NotNull
  public UUID eventId;

  @NotBlank
  @Size(min = 3, max = 64)
  @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
  public String projectId;

  @NotBlank
  @Size(min = 3, max = 128)
  @Pattern(regexp = "^[a-zA-Z0-9./:_-]+$")
  public String artifactId;

  @NotBlank
  public String source;

  @NotNull
  public Instant timestamp;

  @NotBlank
  public String type;

  @NotNull
  public Map<String, Object> payload;
}
