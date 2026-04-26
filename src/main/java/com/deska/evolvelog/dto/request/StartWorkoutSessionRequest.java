package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartWorkoutSessionRequest(
        @NotNull(message = "Training plan ID is required")
        UUID trainingPlanId
) {}
