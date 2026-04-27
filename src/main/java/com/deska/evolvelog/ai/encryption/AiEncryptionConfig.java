package com.deska.evolvelog.ai.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Reads the AES-256 encryption key and exposes it as a {@link SecretKey} bean.
 * Fails application startup if the key is missing or not exactly 32 bytes (256 bits).
 *
 * <p>The key is resolved via Spring's property system, so it can be supplied as:
 * <ul>
 *   <li>Spring property: {@code ai.encryption.key=<base64>} in {@code application-local.properties}</li>
 *   <li>Environment variable: {@code AI_ENCRYPTION_KEY=<base64>} (Spring relaxed binding)</li>
 * </ul>
 * Generate a key: {@code openssl rand -base64 32}
 */
@Configuration
public class AiEncryptionConfig {

    private static final Logger log = LoggerFactory.getLogger(AiEncryptionConfig.class);
    private static final int REQUIRED_KEY_BYTES = 32;

    @Bean
    public SecretKey aiEncryptionKey(@Value("${ai.encryption.key}") String rawKey) {
        if (rawKey.isBlank()) {
            throw new IllegalStateException(
                    "ai.encryption.key / AI_ENCRYPTION_KEY is blank. " +
                    "Generate a 32-byte key: openssl rand -base64 32");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(rawKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ai.encryption.key is not valid Base64", e);
        }
        if (keyBytes.length != REQUIRED_KEY_BYTES) {
            throw new IllegalStateException(
                    "ai.encryption.key must be exactly 32 bytes (256 bits) when Base64-decoded. " +
                    "Got " + keyBytes.length + " bytes.");
        }
        log.info("AI encryption key loaded successfully (256-bit AES)");
        return new SecretKeySpec(keyBytes, "AES");
    }
}
