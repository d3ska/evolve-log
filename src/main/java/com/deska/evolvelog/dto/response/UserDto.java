package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.UnitSystem;
import com.deska.evolvelog.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name,
        UnitSystem unitSystem,
        String locale,
        LocalDateTime createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getUnitSystem(),
                user.getLocale(),
                user.getCreatedAt()
        );
    }
}
