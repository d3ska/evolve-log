package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LogSupplementRequest(
        @NotNull UUID supplementId,
        UUID planEntryId,
        OffsetDateTime takenAt,
        BigDecimal doseAmount,
        String doseUnit,
        String notes
) {}
