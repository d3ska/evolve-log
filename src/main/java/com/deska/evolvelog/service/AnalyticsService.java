package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.dto.response.AnalyticsDto;
import com.deska.evolvelog.dto.response.ExerciseProgressDto;
import com.deska.evolvelog.dto.response.ExerciseProgressPointDto;
import com.deska.evolvelog.dto.response.MeasurementTrendDto;
import com.deska.evolvelog.dto.response.PersonalRecordDto;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MeasurementRepository measurementRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional(readOnly = true)
    public AnalyticsDto getAnalytics(UUID userId, LocalDate from, LocalDate to) {
        List<Measurement> measurements = measurementRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        List<MeasurementTrendDto> weightTrend = measurements.stream()
                .filter(m -> m.getWeightKg() != null)
                .map(MeasurementTrendDto::from)
                .toList();

        List<MeasurementTrendDto> bodyFatTrend = measurements.stream()
                .filter(m -> m.getBodyFatPercent() != null)
                .map(MeasurementTrendDto::from)
                .toList();

        List<PersonalRecordDto> personalRecords = exerciseRepository
                .findPersonalRecordsByUserId(userId);

        BigDecimal weightChange = computeChange(
                measurements.stream().filter(m -> m.getWeightKg() != null).toList(),
                Measurement::getWeightKg
        );
        BigDecimal bodyFatChange = computeChange(
                measurements.stream().filter(m -> m.getBodyFatPercent() != null).toList(),
                Measurement::getBodyFatPercent
        );

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        int totalWorkouts = (int) workoutSessionRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end)
                .size();

        return new AnalyticsDto(
                weightTrend,
                bodyFatTrend,
                personalRecords,
                weightChange,
                bodyFatChange,
                totalWorkouts
        );
    }

    @Transactional(readOnly = true)
    public ExerciseProgressDto getExerciseProgress(UUID userId, String exerciseName, @Nullable UUID planId) {
        List<ExerciseProgressPointDto> history = planId != null
                ? exerciseRepository.findProgressByUserIdAndExerciseNameAndPlanId(userId, exerciseName, planId)
                : exerciseRepository.findProgressByUserIdAndExerciseName(userId, exerciseName);

        PersonalRecordDto pr = history.stream()
                .filter(p -> p.weightKg() != null)
                .max(Comparator.comparing(ExerciseProgressPointDto::weightKg))
                .map(p -> new PersonalRecordDto(exerciseName, p.weightKg(), p.sets(), p.reps(), p.date()))
                .orElse(null);

        return new ExerciseProgressDto(exerciseName, history, pr);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctExerciseNames(UUID userId) {
        return exerciseRepository.findDistinctExerciseNamesByUserId(userId);
    }

    private BigDecimal computeChange(List<Measurement> measurements,
                                     java.util.function.Function<Measurement, BigDecimal> extractor) {
        if (measurements.size() < 2) {
            return null;
        }
        Measurement first = measurements.stream()
                .min(Comparator.comparing(Measurement::getDate))
                .orElseThrow();
        Measurement last = measurements.stream()
                .max(Comparator.comparing(Measurement::getDate))
                .orElseThrow();
        BigDecimal firstVal = extractor.apply(first);
        BigDecimal lastVal = extractor.apply(last);
        if (firstVal == null || lastVal == null) {
            return null;
        }
        return lastVal.subtract(firstVal);
    }
}
