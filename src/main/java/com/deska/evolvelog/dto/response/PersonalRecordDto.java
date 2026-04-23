package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PersonalRecordDto(
        String exerciseName,
        BigDecimal maxWeightKg,
        Integer setsAtMax,
        Integer repsAtMax,
        LocalDateTime achievedAt
) {}
