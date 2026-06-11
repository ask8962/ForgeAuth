package com.forgeauth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RsaKeyManager {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyManager.class);

    @Value("${forgeauth.jwt.key-store-path:./forgeauth-keys.json}")
    private String keyStorePath;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private String keyId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        File keyFile = new File(keyStorePath);
        if (keyFile.exists()) {
            loadKeys(keyFile);
        } else {
            generateAndSaveKeys(keyFile);
        }
    }

    private void loadKeys(File keyFile) {
        try {
            log.info("Loading RSA keys from {}", keyFile.getAbsolutePath());
            Map<String, String> keyData = objectMapper.readValue(keyFile, Map.class);
            this.keyId = keyData.get("keyId");
            
            byte[] pubBytes = Base64.getDecoder().decode(keyData.get("publicKey"));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
            
            byte[] privBytes = Base64.getDecoder().decode(keyData.get("privateKey"));
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        } catch (Exception e) {
            log.error("Failed to load RSA keys", e);
            throw new RuntimeException("Could not load RSA keys", e);
        }
    }

    private void generateAndSaveKeys(File keyFile) {
        try {
            log.info("Generating new RSA keys and saving to {}", keyFile.getAbsolutePath());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.keyId = UUID.randomUUID().toString();
            
            Map<String, String> keyData = new HashMap<>();
            keyData.put("keyId", this.keyId);
            keyData.put("publicKey", Base64.getEncoder().encodeToString(this.publicKey.getEncoded()));
            keyData.put("privateKey", Base64.getEncoder().encodeToString(this.privateKey.getEncoded()));
            
            File parentDir = keyFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(keyFile, keyData);
        } catch (Exception e) {
            log.error("Failed to generate RSA keys", e);
            throw new RuntimeException("Could not generate RSA keys", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
