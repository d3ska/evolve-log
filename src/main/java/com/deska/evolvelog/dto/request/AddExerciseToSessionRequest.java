package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddExerciseToSessionRequest(
        @NotNull UUID sessionId,
        @NotBlank String name,
        @NotNull @Min(1) Integer sets
) {}
