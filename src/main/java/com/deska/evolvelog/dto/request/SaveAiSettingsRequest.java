package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SaveAiSettingsRequest(
        @NotBlank(message = "Provider is required")
        String provider,

        @NotBlank(message = "API key is required")
        String apiKey
) {}
