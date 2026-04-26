package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;

public record MuscleGroupVolumeDto(
        String muscle,
        BigDecimal volumeLoad,
        BigDecimal internalLoad,
        int exerciseCount
) {}
