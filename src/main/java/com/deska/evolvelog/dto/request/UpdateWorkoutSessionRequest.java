package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateWorkoutSessionRequest(
        @PastOrPresent(message = "Date cannot be in the future")
        LocalDateTime date,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        String notes,

        UUID trainingPlanId
) {}
