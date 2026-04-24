package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.SupplementPlan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SupplementPlanDto(
        UUID id,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        List<SupplementPlanEntryDto> entries
) {
    public static SupplementPlanDto from(SupplementPlan p) {
        List<SupplementPlanEntryDto> entries = p.getEntries().stream()
                .map(SupplementPlanEntryDto::from)
                .toList();
        return new SupplementPlanDto(p.getId(), p.getName(), p.getDescription(), p.isActive(), p.getCreatedAt(), entries);
    }
}
