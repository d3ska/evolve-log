package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.WithingsToken;

import java.time.OffsetDateTime;

public record WithingsStatusDto(
        boolean connected,
        OffsetDateTime lastSyncAt
) {
    public static WithingsStatusDto connected(OffsetDateTime lastSyncAt) {
        return new WithingsStatusDto(true, lastSyncAt);
    }

    public static WithingsStatusDto disconnected() {
        return new WithingsStatusDto(false, null);
    }

    public static WithingsStatusDto from(WithingsToken token) {
        return new WithingsStatusDto(true, token.getUpdatedAt());
    }
}
