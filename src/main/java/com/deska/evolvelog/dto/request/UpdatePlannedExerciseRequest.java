package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdatePlannedExerciseRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Min(value = 1, message = "Sets must be at least 1")
        Integer sets,

        @Min(value = 1, message = "Reps min must be at least 1")
        Integer repsMin,

        @Min(value = 1, message = "Reps max must be at least 1")
        Integer repsMax,

        @Min(value = 0, message = "Rest seconds must be non-negative")
        Integer restSeconds,

        Integer position,

        String notes,

        UUID exerciseDefinitionId
) {}
