package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WithingsExchangeRequest(
        @NotBlank String code
) {}
