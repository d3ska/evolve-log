package com.deska.evolvelog.dto.request;

import java.math.BigDecimal;

public record PatchWorkoutSetRequest(
        Integer reps,
        BigDecimal weightKg,
        Boolean completed
) {}
