package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.TimeSlot;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SupplementTodayDto(
        UUID planId,
        String planName,
        List<TodayEntryDto> entries
) {
    public record TodayEntryDto(
            UUID entryId,
            UUID supplementId,
            String supplementName,
            TimeSlot timeSlot,
            BigDecimal doseAmount,
            String doseUnit,
            boolean takenToday,
            OffsetDateTime loggedAt,
            UUID logId
    ) {}
}
