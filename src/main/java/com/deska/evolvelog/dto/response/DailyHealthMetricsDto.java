package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record DailyHealthMetricsDto(
        LocalDate date,
        String source,
        Map<String, BigDecimal> metrics
) {}
