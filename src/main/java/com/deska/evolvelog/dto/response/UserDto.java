package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.UnitSystem;
import com.deska.evolvelog.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        UnitSystem unitSystem,
        LocalDateTime createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getUnitSystem(), user.getCreatedAt());
    }
}
