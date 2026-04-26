package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateExerciseRequest(
        @NotBlank(message = "Exercise name is required")
        String name,

        @NotNull(message = "Sets are required")
        @Min(value = 1, message = "Sets must be at least 1")
        Integer sets,

        @NotNull(message = "Reps are required")
        @Min(value = 1, message = "Reps must be at least 1")
        Integer reps,

        @DecimalMin(value = "0.0", message = "Weight must be non-negative")
        BigDecimal weightKg,

        String notes,

        Integer position,

        java.util.UUID exerciseDefinitionId,

        @jakarta.validation.constraints.DecimalMin(value = "1.0", message = "RPE must be between 1.0 and 10.0")
        @jakarta.validation.constraints.DecimalMax(value = "10.0", message = "RPE must be between 1.0 and 10.0")
        java.math.BigDecimal rpe
) {}
