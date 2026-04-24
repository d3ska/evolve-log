package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.Supplement;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplementDto(
        UUID id,
        String name,
        String brand,
        String form,
        String notes,
        OffsetDateTime createdAt
) {
    public static SupplementDto from(Supplement s) {
        return new SupplementDto(s.getId(), s.getName(), s.getBrand(), s.getForm(), s.getNotes(), s.getCreatedAt());
    }
}
