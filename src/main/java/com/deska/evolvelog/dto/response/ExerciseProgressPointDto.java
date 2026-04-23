package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExerciseProgressPointDto(
        LocalDateTime date,
        BigDecimal weightKg,
        Integer sets,
        Integer reps,
        UUID sessionId
) {}
