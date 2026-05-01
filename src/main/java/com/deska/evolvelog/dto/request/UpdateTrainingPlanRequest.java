package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;

public record UpdateTrainingPlanRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        String description,

        DayOfWeek dayOfWeek,

        Boolean isActive,

        List<CreatePlannedExerciseRequest> plannedExercises
) {}
