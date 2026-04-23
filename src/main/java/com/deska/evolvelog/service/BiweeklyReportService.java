package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.BiweeklyReportDto;
import com.deska.evolvelog.dto.response.MediaAttachmentDto;
import com.deska.evolvelog.dto.response.MeasurementTrendDto;
import com.deska.evolvelog.dto.response.PersonalRecordDto;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BiweeklyReportService {

    private final MeasurementRepository measurementRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final MediaAttachmentService mediaAttachmentService;

    @Transactional(readOnly = true)
    public BiweeklyReportDto generateReport(UUID userId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<Measurement> measurements = measurementRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        MeasurementTrendDto startMeasurement = measurements.isEmpty()
                ? null : MeasurementTrendDto.from(measurements.getFirst());
        MeasurementTrendDto endMeasurement = measurements.isEmpty()
                ? null : MeasurementTrendDto.from(measurements.getLast());

        BigDecimal weightChange = computeDelta(startMeasurement, endMeasurement,
                MeasurementTrendDto::weightKg);
        BigDecimal bodyFatChange = computeDelta(startMeasurement, endMeasurement,
                MeasurementTrendDto::bodyFatPercent);

        List<MeasurementTrendDto> measurementTimeline = measurements.stream()
                .map(MeasurementTrendDto::from)
                .toList();

        List<WorkoutSession> sessions = workoutSessionRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end);

        int totalSets = sessions.stream()
                .flatMap(s -> s.getExercises().stream())
                .mapToInt(e -> e.getSets() != null ? e.getSets() : 0)
                .sum();

        int totalDuration = sessions.stream()
                .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();

        List<WorkoutSessionDto> sessionDtos = sessions.stream()
                .map(WorkoutSessionDto::from)
                .toList();

        List<PersonalRecordDto> newPersonalRecords = exerciseRepository
                .findPersonalRecordsByUserIdAndDateRange(userId, start, end);

        List<MediaAttachmentDto> mediaAttachments = mediaAttachmentService
                .findByDateRange(userId, from, to)
                .stream()
                .map(MediaAttachmentDto::from)
                .toList();

        return new BiweeklyReportDto(
                from,
                to,
                startMeasurement,
                endMeasurement,
                weightChange,
                bodyFatChange,
                measurementTimeline,
                sessions.size(),
                totalSets,
                totalDuration,
                sessionDtos,
                newPersonalRecords,
                mediaAttachments
        );
    }

    private BigDecimal computeDelta(MeasurementTrendDto start, MeasurementTrendDto end,
                                    java.util.function.Function<MeasurementTrendDto, BigDecimal> extractor) {
        if (start == null || end == null) return null;
        BigDecimal startVal = extractor.apply(start);
        BigDecimal endVal = extractor.apply(end);
        if (startVal == null || endVal == null) return null;
        return endVal.subtract(startVal);
    }
}
