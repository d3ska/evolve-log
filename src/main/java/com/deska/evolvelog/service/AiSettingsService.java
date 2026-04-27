package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.AiSettings;
import com.deska.evolvelog.repository.AiSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AiSettingsService {

    private final AiSettingsRepository aiSettingsRepository;

    public AiSettingsService(AiSettingsRepository aiSettingsRepository) {
        this.aiSettingsRepository = aiSettingsRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AiSettings> getSettings(UUID userId) {
        return aiSettingsRepository.findById(userId);
    }

    /**
     * Saves (or replaces) the API key for the user.
     * JPA's {@code @Convert} on the entity encrypts the key before persisting.
     *
     * @param apiKey plain-text API key — encrypted at rest via AES-256-GCM
     */
    @Transactional
    public AiSettings saveSettings(UUID userId, String provider, String apiKey) {
        AiSettings existing = aiSettingsRepository.findById(userId).orElse(null);
        if (existing != null) {
            existing.updateKey(provider, apiKey);
            return aiSettingsRepository.save(existing);
        }
        AiSettings settings = AiSettings.builder()
                .userId(userId)
                .provider(provider)
                .apiKeyEncrypted(apiKey)
                .build();
        return aiSettingsRepository.save(settings);
    }

    /**
     * Returns the decrypted API key.
     * JPA's {@code @Convert} decrypts the stored value automatically.
     * Returns empty if no key is configured.
     */
    @Transactional(readOnly = true)
    public Optional<String> getDecryptedApiKey(UUID userId) {
        return aiSettingsRepository.findById(userId)
                .map(AiSettings::getApiKeyEncrypted);
    }
}
