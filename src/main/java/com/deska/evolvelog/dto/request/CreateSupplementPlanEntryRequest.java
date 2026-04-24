package com.deska.evolvelog.dto.request;

import com.deska.evolvelog.domain.TimeSlot;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSupplementPlanEntryRequest(
        @NotNull UUID supplementId,
        @NotNull TimeSlot timeSlot,
        String customTime,
        BigDecimal doseAmount,
        String doseUnit,
        String notes,
        Integer sortOrder
) {}
