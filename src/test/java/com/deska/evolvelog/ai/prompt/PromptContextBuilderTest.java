package com.deska.evolvelog.ai.prompt;

import com.deska.evolvelog.domain.BloodTestReport;
import com.deska.evolvelog.domain.BloodTestResult;
import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptContextBuilderTest {

    @Mock
    private WorkoutSessionRepository workoutSessionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private BloodTestReportRepository bloodTestReportRepository;

    @Mock
    private MeasurementRepository measurementRepository;

    @InjectMocks
    private PromptContextBuilder contextBuilder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    // --- dashboard context ---

    @Test
    void shouldFetchRecentWorkoutsAndLatestMeasurementWhenContextIsDashboard() {
        // given
        WorkoutSession session = WorkoutSession.builder()
                .date(LocalDateTime.of(2025, 1, 10, 9, 0))
                .durationMinutes(60)
                .exercises(new ArrayList<>())
                .build();
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId))
                .thenReturn(Optional.empty());

        // when
        String context = contextBuilder.buildContext("dashboard", userId);

        // then
        assertThat(context).contains("Recent Workouts");
        assertThat(context).contains("2025-01-10");
        verify(workoutSessionRepository).findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class));
        verify(measurementRepository).findFirstByUserIdOrderByDateDesc(userId);
    }

    @Test
    void shouldUseDefaultDashboardContextWhenPageContextIsNull() {
        // given
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId))
                .thenReturn(Optional.empty());

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("Recent Workouts");
    }

    @Test
    void shouldUseDefaultDashboardContextWhenPageContextIsBlank() {
        // given
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId))
                .thenReturn(Optional.empty());

        // when
        String context = contextBuilder.buildContext("  ", userId);

        // then
        assertThat(context).contains("Recent Workouts");
    }

    // --- workouts context ---

    @Test
    void shouldFetchLast30DaysWorkoutsWhenContextIsWorkouts() {
        // given
        when(workoutSessionRepository.findByUserIdAndDateBetweenOrderByDateAsc(eq(userId), any(), any()))
                .thenReturn(List.of());

        // when
        String context = contextBuilder.buildContext("workouts", userId);

        // then
        assertThat(context).contains("Workouts");
        verify(workoutSessionRepository).findByUserIdAndDateBetweenOrderByDateAsc(eq(userId), any(), any());
    }

    // --- progress context ---

    @Test
    void shouldFetchExerciseNamesAndPersonalRecordsWhenContextIsProgress() {
        // given
        when(exerciseRepository.findDistinctExerciseNamesByUserId(userId))
                .thenReturn(List.of("Bench Press", "Squat"));
        when(exerciseRepository.findPersonalRecordsByUserId(userId))
                .thenReturn(List.of());

        // when
        String context = contextBuilder.buildContext("progress", userId);

        // then
        assertThat(context).contains("Bench Press");
        assertThat(context).contains("Squat");
        assertThat(context).contains("Personal Records");
        verify(exerciseRepository).findDistinctExerciseNamesByUserId(userId);
        verify(exerciseRepository).findPersonalRecordsByUserId(userId);
    }

    // --- blood context ---

    @Test
    void shouldFetchBloodReportsWithResultsWhenContextIsBlood() {
        // given
        BloodTestResult result = BloodTestResult.builder()
                .parameterLabel("Hemoglobin")
                .value(new BigDecimal("14.5"))
                .unit("g/dL")
                .refLow(new BigDecimal("13.0"))
                .refHigh(new BigDecimal("17.5"))
                .build();

        BloodTestReport report = BloodTestReport.builder()
                .date(LocalDate.of(2025, 1, 5))
                .labName("LabCorp")
                .results(List.of(result))
                .build();

        when(bloodTestReportRepository.findByUserIdOrderByDateDesc(userId))
                .thenReturn(List.of(report));

        // when
        String context = contextBuilder.buildContext("blood", userId);

        // then
        assertThat(context).contains("Blood Test Reports");
        assertThat(context).contains("LabCorp");
        assertThat(context).contains("Hemoglobin");
        assertThat(context).contains("14.5");
        verify(bloodTestReportRepository).findByUserIdOrderByDateDesc(userId);
    }

    // --- measurements context ---

    @Test
    void shouldFetchMeasurementsForLastSixMonthsWhenContextIsMeasurements() {
        // given
        Measurement m = Measurement.builder()
                .date(LocalDate.of(2025, 1, 1))
                .weightKg(new BigDecimal("80.5"))
                .bodyFatPercent(new BigDecimal("15.2"))
                .build();
        when(measurementRepository.findByUserIdAndDateBetweenOrderByDateAsc(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(m));

        // when
        String context = contextBuilder.buildContext("measurements", userId);

        // then
        assertThat(context).contains("Body Measurements");
        assertThat(context).contains("80.5");
        assertThat(context).contains("15.2");
        verify(measurementRepository).findByUserIdAndDateBetweenOrderByDateAsc(eq(userId), any(LocalDate.class), any(LocalDate.class));
    }

    // --- empty state ---

    @Test
    void shouldReturnNoWorkoutsMessageWhenNoSessionsExist() {
        // given
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId))
                .thenReturn(Optional.empty());

        // when
        String context = contextBuilder.buildContext("dashboard", userId);

        // then
        assertThat(context).contains("No workouts recorded");
    }

    @Test
    void shouldReturnNoMeasurementsMessageWhenMeasurementAbsent() {
        // given
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId))
                .thenReturn(Optional.empty());

        // when
        String context = contextBuilder.buildContext("dashboard", userId);

        // then
        assertThat(context).contains("No measurements recorded");
    }
}
