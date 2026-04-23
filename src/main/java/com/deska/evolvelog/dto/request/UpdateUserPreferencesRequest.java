package com.deska.evolvelog.dto.request;

import com.deska.evolvelog.domain.UnitSystem;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferencesRequest(
        @NotNull(message = "Unit system is required")
        UnitSystem unitSystem
) {}
