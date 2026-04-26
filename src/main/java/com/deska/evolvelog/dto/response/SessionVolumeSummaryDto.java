package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SessionVolumeSummaryDto(
        UUID sessionId,
        BigDecimal totalVolumeLoad,
        BigDecimal totalInternalLoad,
        double rpeCompleteness,
        int exerciseCount,
        List<MuscleGroupVolumeDto> byMuscleGroup
) {}
