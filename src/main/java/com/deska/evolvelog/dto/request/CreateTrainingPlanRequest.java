package com.deska.evolvelog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public record CreateTrainingPlanRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        String description,

        DayOfWeek dayOfWeek,

        UUID blockId,

        @Valid
        List<CreatePlannedExerciseRequest> plannedExercises
) {}
