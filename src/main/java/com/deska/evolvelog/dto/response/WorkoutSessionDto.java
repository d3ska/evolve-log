package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        WorkoutSessionStatus status,
        Integer deviationCount,
        List<PlanSnapshotEntryDto> planSnapshot
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static WorkoutSessionDto from(WorkoutSession session) {
        List<PlanSnapshotEntryDto> snapshot = parseSnapshot(session.getPlanSnapshot());
        Integer deviationCount = computeDeviationCount(session, snapshot);
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
                session.getStatus(),
                deviationCount,
                snapshot
        );
    }

    public static WorkoutSessionDto summary(WorkoutSession session) {
        List<PlanSnapshotEntryDto> snapshot = parseSnapshot(session.getPlanSnapshot());
        Integer deviationCount = computeDeviationCount(session, snapshot);
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
                session.getStatus(),
                deviationCount,
                snapshot
        );
    }

    private static List<PlanSnapshotEntryDto> parseSnapshot(String json) {
        if (json == null) {
            return null;
        }
        try {
            List<Map<String, Object>> raw = MAPPER.readValue(json, new TypeReference<>() {});
            return raw.stream().map(m -> new PlanSnapshotEntryDto(
                    m.get("plannedExerciseId") != null ? UUID.fromString((String) m.get("plannedExerciseId")) : null,
                    (String) m.get("name"),
                    (Integer) m.get("sets"),
                    (Integer) m.get("repsMin"),
                    (Integer) m.get("repsMax"),
                    (Integer) m.get("restSeconds"),
                    (Integer) m.get("position")
            )).toList();
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer computeDeviationCount(WorkoutSession session, List<PlanSnapshotEntryDto> snapshot) {
        if (snapshot == null) {
            return null;
        }
        List<Exercise> exercises = session.getExercises();
        Set<UUID> linkedPlannedIds = exercises.stream()
                .filter(e -> e.getPlannedExerciseId() != null)
                .map(Exercise::getPlannedExerciseId)
                .collect(Collectors.toSet());

        long skipped = snapshot.stream()
                .filter(s -> !linkedPlannedIds.contains(s.plannedExerciseId()))
                .count();
        long added = exercises.stream()
                .filter(e -> e.getPlannedExerciseId() == null)
                .count();

        return (int) (skipped + added);
    }
}
