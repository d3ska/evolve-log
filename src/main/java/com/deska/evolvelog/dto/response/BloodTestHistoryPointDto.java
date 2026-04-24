package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.BloodTestResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BloodTestHistoryPointDto(
        LocalDate date,
        BigDecimal value,
        String unit,
        BigDecimal refLow,
        BigDecimal refHigh,
        String flag
) {
    public static BloodTestHistoryPointDto from(BloodTestResult r) {
        return new BloodTestHistoryPointDto(
                r.getReport().getDate(),
                r.getValue(),
                r.getUnit(),
                r.getRefLow(),
                r.getRefHigh(),
                r.getFlag()
        );
    }
}
