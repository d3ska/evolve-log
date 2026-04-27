package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.AiSettings;

public record AiSettingsView(
        boolean hasApiKey,
        String provider,
        String apiKeyHint
) {
    public static AiSettingsView from(AiSettings settings) {
        String key = settings.getApiKeyEncrypted();
        boolean hasKey = key != null && !key.isBlank();
        String hint = hasKey ? maskKey(key) : null;
        return new AiSettingsView(hasKey, settings.getProvider(), hint);
    }

    public static AiSettingsView empty() {
        return new AiSettingsView(false, null, null);
    }

    private static String maskKey(String key) {
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
