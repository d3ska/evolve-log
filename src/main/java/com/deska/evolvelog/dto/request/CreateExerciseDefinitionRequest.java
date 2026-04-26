package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseDefinitionRequest(
        @NotBlank(message = "Exercise name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Primary muscle is required")
        @Size(max = 50, message = "Primary muscle must be at most 50 characters")
        String primaryMuscle,

        @Size(max = 50, message = "Equipment must be at most 50 characters")
        String equipment
) {}
