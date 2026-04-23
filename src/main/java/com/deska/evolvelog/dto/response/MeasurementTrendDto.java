package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.Measurement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeasurementTrendDto(
        LocalDate date,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal chestCm,
        BigDecimal waistNarrowestCm,
        BigDecimal waistNavelCm,
        BigDecimal bicepsCm,
        BigDecimal thighCm,
        BigDecimal calvesCm
) {
    public static MeasurementTrendDto from(Measurement m) {
        return new MeasurementTrendDto(
                m.getDate(),
                m.getWeightKg(),
                m.getBodyFatPercent(),
                m.getChestCm(),
                m.getWaistNarrowestCm(),
                m.getWaistNavelCm(),
                m.getBicepsCm(),
                m.getThighCm(),
                m.getCalvesCm()
        );
    }
}
