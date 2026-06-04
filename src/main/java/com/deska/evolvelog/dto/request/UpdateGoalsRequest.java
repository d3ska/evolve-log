package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateGoalsRequest(
        @Size(max = 2000, message = "Goals must not exceed 2000 characters")
        String goals
) {
}
