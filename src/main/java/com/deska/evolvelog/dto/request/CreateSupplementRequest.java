package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplementRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String brand,
        @Size(max = 100) String form,
        String notes
) {}
