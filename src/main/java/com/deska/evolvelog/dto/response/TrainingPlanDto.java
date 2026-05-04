package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.TrainingPlan;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TrainingPlanDto(
        UUID id,
        String name,
        String description,
        DayOfWeek dayOfWeek,
        boolean isActive,
        UUID blockId,
        List<PlannedExerciseDto> plannedExercises,
        LocalDateTime createdAt
) {
    public static TrainingPlanDto from(TrainingPlan plan) {
        return new TrainingPlanDto(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getDayOfWeek(),
                plan.isActive(),
                plan.getBlockId(),
                plan.getPlannedExercises().stream().map(PlannedExerciseDto::from).toList(),
                plan.getCreatedAt()
        );
    }

    public static TrainingPlanDto summary(TrainingPlan plan) {
        return new TrainingPlanDto(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getDayOfWeek(),
                plan.isActive(),
                plan.getBlockId(),
                List.of(),
                plan.getCreatedAt()
        );
    }
}
