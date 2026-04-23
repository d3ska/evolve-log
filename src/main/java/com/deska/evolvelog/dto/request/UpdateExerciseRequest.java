package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateExerciseRequest(
        String name,

        @Min(value = 1, message = "Sets must be at least 1")
        Integer sets,

        @Min(value = 1, message = "Reps must be at least 1")
        Integer reps,

        @DecimalMin(value = "0.0", message = "Weight must be non-negative")
        BigDecimal weightKg,

        String notes,

        Integer position
) {}
