package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.WithingsMeasurement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WithingsMeasurementDto(
        LocalDate date,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal muscleMassKg,
        BigDecimal boneMassKg
) {
    public static WithingsMeasurementDto from(WithingsMeasurement m) {
        return new WithingsMeasurementDto(
                m.getDate(),
                m.getWeightKg(),
                m.getBodyFatPercent(),
                m.getMuscleMassKg(),
                m.getBoneMassKg()
        );
    }
}
