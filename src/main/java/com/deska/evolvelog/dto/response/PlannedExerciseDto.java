package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.PlannedExercise;

import java.util.UUID;

public record PlannedExerciseDto(
        UUID id,
        String name,
        Integer sets,
        Integer repsMin,
        Integer repsMax,
        Integer restSeconds,
        Integer position,
        String notes
) {
    public static PlannedExerciseDto from(PlannedExercise e) {
        return new PlannedExerciseDto(
                e.getId(),
                e.getName(),
                e.getSets(),
                e.getRepsMin(),
                e.getRepsMax(),
                e.getRestSeconds(),
                e.getPosition(),
                e.getNotes()
        );
    }
}
