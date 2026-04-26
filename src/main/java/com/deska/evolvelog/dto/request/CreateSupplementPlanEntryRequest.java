package com.deska.evolvelog.dto.request;

import com.deska.evolvelog.domain.TimeSlot;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSupplementPlanEntryRequest(
        @NotNull UUID supplementId,
        @NotNull TimeSlot timeSlot,
        @Size(max = 20) String customTime,
        @DecimalMin(value = "0.0", message = "Dose amount must be non-negative") BigDecimal doseAmount,
        @Size(max = 50) String doseUnit,
        String notes,
        Integer sortOrder
) {}
