package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.TrainingBlock;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TrainingBlockDto(
        UUID id,
        String name,
        String description,
        boolean isActive,
        OffsetDateTime createdAt
) {
    public static TrainingBlockDto from(TrainingBlock block) {
        return new TrainingBlockDto(
                block.getId(),
                block.getName(),
                block.getDescription(),
                block.isActive(),
                block.getCreatedAt()
        );
    }
}
