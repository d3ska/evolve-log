package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateLocaleRequest(
        @NotBlank(message = "Locale is required")
        @Pattern(regexp = "^(en|pl)$", message = "Supported locales: en, pl")
        String locale
) {}
