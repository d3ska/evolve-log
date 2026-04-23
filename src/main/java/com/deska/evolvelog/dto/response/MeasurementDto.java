package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.Measurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeasurementDto(
        UUID id,
        LocalDate date,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal chestCm,
        BigDecimal waistNarrowestCm,
        BigDecimal waistNavelCm,
        BigDecimal bicepsCm,
        BigDecimal thighCm,
        BigDecimal calvesCm,
        String notes,
        LocalDateTime createdAt
) {
    public static MeasurementDto from(Measurement m) {
        return new MeasurementDto(
                m.getId(),
                m.getDate(),
                m.getWeightKg(),
                m.getBodyFatPercent(),
                m.getChestCm(),
                m.getWaistNarrowestCm(),
                m.getWaistNavelCm(),
                m.getBicepsCm(),
                m.getThighCm(),
                m.getCalvesCm(),
                m.getNotes(),
                m.getCreatedAt()
        );
    }
}
