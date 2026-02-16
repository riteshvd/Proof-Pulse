package com.proofpulse.ledger.attest;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class KeyManager {

  // For dev: auto-generate keypair on startup if env vars not provided.
  // Later: load from AWS KMS / Secrets Manager.
  private final KeyPair keyPair;

  public KeyManager() {
    try {
      String priv = System.getenv("PP_ATTEST_PRIVATE_KEY_B64");
      String pub = System.getenv("PP_ATTEST_PUBLIC_KEY_B64");

      if (priv != null && pub != null) {
        this.keyPair = new KeyPair(loadPublic(pub), loadPrivate(priv));
      } else {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        this.keyPair = kpg.generateKeyPair();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize KeyManager", e);
    }
  }

  public PublicKey publicKey() {
    return keyPair.getPublic();
  }

  public PrivateKey privateKey() {
    return keyPair.getPrivate();
  }

  public String publicKeyBase64() {
    return Base64.getEncoder().encodeToString(publicKey().getEncoded());
  }

  private PublicKey loadPublic(String b64) throws Exception {
    byte[] bytes = Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
    return KeyFactory.getInstance("Ed25519").generatePublic(spec);
  }

  private PrivateKey loadPrivate(String b64) throws Exception {
    byte[] bytes = Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
    return KeyFactory.getInstance("Ed25519").generatePrivate(spec);
  }
}
