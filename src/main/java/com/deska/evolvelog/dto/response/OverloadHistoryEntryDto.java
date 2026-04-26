package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OverloadHistoryEntryDto(
        LocalDate sessionDate,
        UUID sessionId,
        Integer sets,
        Integer reps,
        BigDecimal weightKg,
        BigDecimal e1Rm,
        BigDecimal volumeLoad,
        BigDecimal rpe,
        boolean isPR,
        BigDecimal volumeDelta
) {}
