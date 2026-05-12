package com.deska.evolvelog.dto.response;

import java.util.UUID;

public record PlanSnapshotEntry(
        UUID plannedExerciseId,
        String name,
        Integer sets,
        Integer repsMin,
        Integer repsMax,
        Integer restSeconds,
        Integer position
) {
}
