package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.SupplementPlanEntry;
import com.deska.evolvelog.domain.TimeSlot;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplementPlanEntryDto(
        UUID id,
        SupplementDto supplement,
        TimeSlot timeSlot,
        String customTime,
        BigDecimal doseAmount,
        String doseUnit,
        String notes,
        int sortOrder
) {
    public static SupplementPlanEntryDto from(SupplementPlanEntry e) {
        return new SupplementPlanEntryDto(
                e.getId(),
                SupplementDto.from(e.getSupplement()),
                e.getTimeSlot(),
                e.getCustomTime(),
                e.getDoseAmount(),
                e.getDoseUnit(),
                e.getNotes(),
                e.getSortOrder()
        );
    }
}
