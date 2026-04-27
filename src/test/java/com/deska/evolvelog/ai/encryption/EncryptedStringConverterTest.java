package com.deska.evolvelog.ai.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        // given — 32 random bytes as test-only AES-256 key
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        converter = new EncryptedStringConverter(secretKey);
    }

    @Test
    void shouldEncryptAndDecryptRoundTrip() {
        // given
        String original = "sk-ant-my-secret-api-key-12345";

        // when
        String encrypted = converter.convertToDatabaseColumn(original);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // then
        assertThat(decrypted).isEqualTo(original);
        assertThat(encrypted).isNotEqualTo(original);
    }

    @Test
    void shouldProduceDifferentCiphertextEachTime() {
        // given
        String original = "same-value";

        // when — each call generates a new random IV
        String enc1 = converter.convertToDatabaseColumn(original);
        String enc2 = converter.convertToDatabaseColumn(original);

        // then
        assertThat(enc1).isNotEqualTo(enc2);
        assertThat(converter.convertToEntityAttribute(enc1)).isEqualTo(original);
        assertThat(converter.convertToEntityAttribute(enc2)).isEqualTo(original);
    }

    @Test
    void shouldReturnNullForNullInput() {
        // when / then
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void shouldThrowWhenCiphertextIsTampered() {
        // given
        String encrypted = converter.convertToDatabaseColumn("secret");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        // flip a byte in the ciphertext portion (after the 12-byte IV)
        raw[raw.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(raw);

        // when / then — GCM authentication tag verification must fail
        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to decrypt");
    }
}
