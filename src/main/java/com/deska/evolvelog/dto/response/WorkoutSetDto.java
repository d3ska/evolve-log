package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.WorkoutSet;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutSetDto(
        UUID id,
        Integer setNumber,
        Integer reps,
        BigDecimal weightKg
) {
    public static WorkoutSetDto from(WorkoutSet s) {
        return new WorkoutSetDto(s.getId(), s.getSetNumber(), s.getReps(), s.getWeightKg());
    }
}
