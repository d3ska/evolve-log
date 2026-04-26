package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateExerciseRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Min(value = 1, message = "Sets must be at least 1")
        Integer sets,

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
