package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.Exercise;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseDto(
        UUID id,
        String name,
        Integer sets,
        Integer reps,
        BigDecimal weightKg,
        String notes,
        Integer position
) {
    public static ExerciseDto from(Exercise e) {
        return new ExerciseDto(
                e.getId(),
                e.getName(),
                e.getSets(),
                e.getReps(),
                e.getWeightKg(),
                e.getNotes(),
                e.getPosition()
        );
    }
}
