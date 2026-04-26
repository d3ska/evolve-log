package com.deska.evolvelog.dto.response;

public record FinishedSessionDto(
        WorkoutSessionDto session,
        SessionVolumeSummaryDto volume
) {}
