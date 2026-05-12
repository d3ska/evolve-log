package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.Exercise;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExerciseDto(
        UUID id,
        String name,
        Integer sets,
        Integer reps,
        BigDecimal weightKg,
        String notes,
        Integer position,
        List<WorkoutSetDto> workoutSets,
        UUID plannedExerciseId
) {
    public static ExerciseDto from(Exercise e) {
        List<WorkoutSetDto> setDtos = e.getWorkoutSets().stream()
                .map(WorkoutSetDto::from)
                .toList();
        return new ExerciseDto(
                e.getId(),
                e.getName(),
                e.getSets(),
                e.getReps(),
                e.getWeightKg(),
                e.getNotes(),
                e.getPosition(),
                setDtos,
                e.getPlannedExerciseId()
        );
    }
}
