package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.AiSettings;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.AiSettingsRepository;
import org.springframework.http.HttpStatus;
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
        if ("anthropic".equals(provider) && !apiKey.startsWith("sk-ant-")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Anthropic API keys must start with 'sk-ant-'");
        }

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
     * Persists the user's fitness goals free-text. Null clears the field.
     * Goals are injected into every AI system prompt by PromptContextBuilder.
     */
    @Transactional
    public void updateGoals(UUID userId, String goals) {
        AiSettings settings = aiSettingsRepository.findById(userId)
                .orElseThrow(() -> new com.deska.evolvelog.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "AI settings not found — save an API key first"));
        settings.updateGoals(goals);
        aiSettingsRepository.save(settings);
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

    /**
     * Returns the provider ID stored for this user (e.g. {@code "anthropic"} or {@code "google"}).
     * Defaults to {@code "anthropic"} when no settings exist.
     */
    @Transactional(readOnly = true)
    public String getProvider(UUID userId) {
        return aiSettingsRepository.findById(userId)
                .map(AiSettings::getProvider)
                .orElse("anthropic");
    }
}
