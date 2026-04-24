package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplementPlanRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
