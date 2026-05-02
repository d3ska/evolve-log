package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WorkoutSessionDto(
        UUID id,
        LocalDateTime date,
        Integer durationMinutes,
        String notes,
        UUID trainingPlanId,
        List<ExerciseDto> exercises,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        WorkoutSessionStatus status
) {
    public static WorkoutSessionDto from(WorkoutSession session) {
        return new WorkoutSessionDto(
                session.getId(),
                session.getDate(),
                session.getDurationMinutes(),
                session.getNotes(),
                session.getTrainingPlan() != null ? session.getTrainingPlan().getId() : null,
                session.getExercises().stream().map(ExerciseDto::from).toList(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getStatus()
        );
    }

    public static WorkoutSessionDto summary(WorkoutSession session) {
        return new WorkoutSessionDto(
                session.getId(),
                session.getDate(),
                session.getDurationMinutes(),
                session.getNotes(),
                session.getTrainingPlan() != null ? session.getTrainingPlan().getId() : null,
                List.of(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getFinishedAt(),
                session.getStatus()
        );
    }
}
