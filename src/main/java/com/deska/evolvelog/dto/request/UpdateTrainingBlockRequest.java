package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateTrainingBlockRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        String description,

        Boolean isActive
) {}
