package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record UpdateTrainingPlanRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        String description,

        DayOfWeek dayOfWeek,

        Boolean isActive,

        // Optional presence: null = field absent (skip), Optional.empty() = explicit null (clear), Optional.of(id) = assign
        Optional<UUID> blockId,

        List<CreatePlannedExerciseRequest> plannedExercises
) {}
