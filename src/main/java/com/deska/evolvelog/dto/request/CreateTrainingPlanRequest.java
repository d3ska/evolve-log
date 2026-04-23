package com.deska.evolvelog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;

public record CreateTrainingPlanRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        String description,

        DayOfWeek dayOfWeek,

        @Valid
        List<CreatePlannedExerciseRequest> plannedExercises
) {}
