package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsDto(
        List<MeasurementTrendDto> weightTrend,
        List<MeasurementTrendDto> bodyFatTrend,
        List<PersonalRecordDto> personalRecords,
        BigDecimal weightChangeSinceStart,
        BigDecimal bodyFatChangeSinceStart,
        Integer totalWorkouts
) {}
