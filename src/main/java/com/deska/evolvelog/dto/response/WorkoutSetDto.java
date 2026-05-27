package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.WorkoutSet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkoutSetDto(
        UUID id,
        UUID exerciseId,
        Integer setNumber,
        Integer reps,
        BigDecimal weightKg,
        boolean completed,
        Instant completedAt
) {
    public static WorkoutSetDto from(WorkoutSet s) {
        return new WorkoutSetDto(
                s.getId(),
                s.getExercise().getId(),
                s.getSetNumber(),
                s.getReps(),
                s.getWeightKg(),
                s.isCompleted(),
                s.getCompletedAt());
    }
}
