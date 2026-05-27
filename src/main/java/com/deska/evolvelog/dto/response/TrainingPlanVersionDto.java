package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.TrainingPlanVersion;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrainingPlanVersionDto(
        int version,
        Instant createdAt,
        int exerciseCount,
        List<PlannedExerciseDto> exercises
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TrainingPlanVersionDto summary(TrainingPlanVersion v) {
        int count = parseCount(v.getExercises());
        return new TrainingPlanVersionDto(v.getVersion(), v.getCreatedAt(), count, null);
    }

    public static TrainingPlanVersionDto full(TrainingPlanVersion v) {
        List<PlannedExerciseDto> exercises = parseExercises(v.getExercises());
        return new TrainingPlanVersionDto(v.getVersion(), v.getCreatedAt(), exercises.size(), exercises);
    }

    private static List<PlannedExerciseDto> parseExercises(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static int parseCount(String json) {
        return parseExercises(json).size();
    }
}
