package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LogSupplementRequest(
        @NotNull UUID supplementId,
        UUID planEntryId,
        OffsetDateTime takenAt,
        @DecimalMin(value = "0.0", message = "Dose amount must be non-negative") BigDecimal doseAmount,
        @Size(max = 50) String doseUnit,
        String notes
) {}
