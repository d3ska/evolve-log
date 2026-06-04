package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SaveAiSettingsRequest(
        @NotBlank(message = "Provider is required")
        @Pattern(regexp = "anthropic|google", message = "Provider must be 'anthropic' or 'google'")
        String provider,

        @NotBlank(message = "API key is required")
        String apiKey
) {}
