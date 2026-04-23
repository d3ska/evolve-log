package com.deska.evolvelog.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BiweeklyReportDto(
        LocalDate from,
        LocalDate to,

        // Measurements
        MeasurementTrendDto startMeasurement,
        MeasurementTrendDto endMeasurement,
        BigDecimal weightChangeKg,
        BigDecimal bodyFatChangePercent,
        List<MeasurementTrendDto> measurementTimeline,

        // Workouts
        Integer totalWorkouts,
        Integer totalExerciseSets,
        Integer totalDurationMinutes,
        List<WorkoutSessionDto> workoutSessions,

        // Personal records achieved in this period
        List<PersonalRecordDto> newPersonalRecords,

        // Media attachments (photos/videos) in this period
        List<MediaAttachmentDto> mediaAttachments
) {}
