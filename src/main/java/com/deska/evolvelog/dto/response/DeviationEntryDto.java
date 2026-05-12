package com.deska.evolvelog.dto.response;

import java.util.UUID;

public record DeviationEntryDto(
        String status,
        UUID plannedExerciseId,
        String name,
        Integer plannedSets,
        Integer plannedRepsMin,
        Integer plannedRepsMax,
        Integer actualSets,
        UUID sessionExerciseId
) {
}
