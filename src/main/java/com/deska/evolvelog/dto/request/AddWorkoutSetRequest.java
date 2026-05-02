package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddWorkoutSetRequest(
        @NotNull UUID exerciseId,
        @NotNull @Min(1) Integer setNumber,
        Integer reps,
        BigDecimal weightKg
) {}
