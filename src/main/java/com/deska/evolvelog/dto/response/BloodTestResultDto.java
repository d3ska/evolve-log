package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.BloodTestResult;

import java.math.BigDecimal;
import java.util.UUID;

public record BloodTestResultDto(
        UUID id,
        String parameterKey,
        String parameterLabel,
        BigDecimal value,
        String unit,
        BigDecimal refLow,
        BigDecimal refHigh,
        String flag,
        String category
) {
    public static BloodTestResultDto from(BloodTestResult r) {
        return new BloodTestResultDto(
                r.getId(),
                r.getParameterKey(),
                r.getParameterLabel(),
                r.getValue(),
                r.getUnit(),
                r.getRefLow(),
                r.getRefHigh(),
                r.getFlag(),
                r.getCategory()
        );
    }
}
