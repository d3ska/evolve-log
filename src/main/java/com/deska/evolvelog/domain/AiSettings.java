package com.deska.evolvelog.domain;

import com.deska.evolvelog.ai.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "anthropic";

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void updateKey(String provider, String apiKeyEncrypted) {
        this.provider = provider;
        this.apiKeyEncrypted = apiKeyEncrypted;
    }
}
