package com.deska.evolvelog.dto.response;

import java.util.List;
import java.util.UUID;

public record ProgressiveOverloadDto(
        UUID exerciseDefinitionId,
        String exerciseName,
        List<OverloadHistoryEntryDto> history
) {}
