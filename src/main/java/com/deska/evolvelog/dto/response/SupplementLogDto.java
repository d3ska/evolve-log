package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.SupplementLog;
import com.deska.evolvelog.domain.SupplementSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplementLogDto(
        UUID id,
        SupplementDto supplement,
        UUID planEntryId,
        OffsetDateTime takenAt,
        BigDecimal doseAmount,
        String doseUnit,
        SupplementSource source,
        String notes
) {
    public static SupplementLogDto from(SupplementLog l) {
        return new SupplementLogDto(
                l.getId(),
                SupplementDto.from(l.getSupplement()),
                l.getPlanEntry() != null ? l.getPlanEntry().getId() : null,
                l.getTakenAt(),
                l.getDoseAmount(),
                l.getDoseUnit(),
                l.getSource(),
                l.getNotes()
        );
    }
}
