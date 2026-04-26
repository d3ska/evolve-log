package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyMuscleVolumeDto(
        LocalDate weekStart,
        String muscle,
        BigDecimal volumeLoad,
        long sessionCount
) {}
