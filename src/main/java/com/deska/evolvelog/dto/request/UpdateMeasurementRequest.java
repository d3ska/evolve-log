package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateMeasurementRequest(
        @PastOrPresent(message = "Date cannot be in the future")
        LocalDate date,

        @DecimalMin(value = "0.1", message = "Weight must be positive")
        BigDecimal weightKg,

        @DecimalMin(value = "0.0", message = "Body fat percent must be non-negative")
        BigDecimal bodyFatPercent,

        @DecimalMin(value = "0.1", message = "Chest measurement must be positive")
        BigDecimal chestCm,

        @DecimalMin(value = "0.1", message = "Waist (narrowest) measurement must be positive")
        BigDecimal waistNarrowestCm,

        @DecimalMin(value = "0.1", message = "Waist (navel) measurement must be positive")
        BigDecimal waistNavelCm,

        @DecimalMin(value = "0.1", message = "Biceps measurement must be positive")
        BigDecimal bicepsCm,

        @DecimalMin(value = "0.1", message = "Thigh measurement must be positive")
        BigDecimal thighCm,

        @DecimalMin(value = "0.1", message = "Calves measurement must be positive")
        BigDecimal calvesCm,

        String notes
) {}
