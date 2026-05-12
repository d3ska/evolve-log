package com.deska.evolvelog.dto.response;

import java.util.List;
import java.util.UUID;

public record SessionDeviationDto(
        UUID sessionId,
        boolean supported,
        List<DeviationEntryDto> entries
) {
}
