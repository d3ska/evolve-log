package com.deska.evolvelog.dto.response;

import java.util.List;
import java.util.UUID;

public record ExerciseDefinitionDto(
        UUID id,
        String name,
        String primaryMuscle,
        String primaryMuscleLabel,
        List<String> secondaryMuscles,
        String equipment,
        boolean isSystem
) {}
