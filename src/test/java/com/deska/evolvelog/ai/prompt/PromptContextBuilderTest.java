package com.deska.evolvelog.ai.prompt;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.repository.AiSettingsRepository;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.FitatuFoodLogRepository;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.deska.evolvelog.repository.SupplementPlanRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.deska.evolvelog.service.HealthMetricService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptContextBuilderTest {

    @Mock private WorkoutSessionRepository workoutSessionRepository;
    @Mock private BloodTestReportRepository bloodTestReportRepository;
    @Mock private MeasurementRepository measurementRepository;
    @Mock private FitatuFoodLogRepository fitatuFoodLogRepository;
    @Mock private HealthMetricService healthMetricService;
    @Mock private AiSettingsRepository aiSettingsRepository;
    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private SupplementPlanRepository supplementPlanRepository;

    @InjectMocks
    private PromptContextBuilder contextBuilder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    /** Stubs all always-called methods with empty/zero defaults (fitatuFoodLog empty → healthMetricService also called). */
    private void stubAllEmpty() {
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId))
                .thenReturn(Optional.empty());
        when(bloodTestReportRepository.countByUserId(userId)).thenReturn(0L);
        when(fitatuFoodLogRepository.findByUserIdAndDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(healthMetricService.getDailyMetrics(eq(userId), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(aiSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
    }

    // --- workout availability ---

    @Test
    void shouldIncludeWorkoutDateWhenSessionExists() {
        // given
        WorkoutSession session = WorkoutSession.builder()
                .date(LocalDateTime.of(2025, 1, 10, 9, 0))
                .build();
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(session)));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId)).thenReturn(Optional.empty());
        when(bloodTestReportRepository.countByUserId(userId)).thenReturn(0L);
        when(fitatuFoodLogRepository.findByUserIdAndDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(healthMetricService.getDailyMetrics(eq(userId), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(aiSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("2025-01-10");
        assertThat(context).contains("Workouts");
    }

    @Test
    void shouldIndicateNoSessionsWhenNoWorkoutsExist() {
        // given
        stubAllEmpty();

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("no sessions recorded yet");
    }

    // --- measurement availability ---

    @Test
    void shouldIncludeMeasurementDateWhenMeasurementExists() {
        // given
        Measurement m = Measurement.builder()
                .date(LocalDate.of(2025, 3, 15))
                .weightKg(new BigDecimal("82.5"))
                .build();
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId)).thenReturn(Optional.of(m));
        when(bloodTestReportRepository.countByUserId(userId)).thenReturn(0L);
        when(fitatuFoodLogRepository.findByUserIdAndDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(healthMetricService.getDailyMetrics(eq(userId), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(aiSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("2025-03-15");
        assertThat(context).contains("measurements");
    }

    @Test
    void shouldIndicateNoMeasurementsWhenNoneExist() {
        // given
        stubAllEmpty();

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("none recorded yet");
    }

    // --- blood test availability ---

    @Test
    void shouldIncludeBloodReportCountWhenReportsExist() {
        // given
        when(workoutSessionRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(measurementRepository.findFirstByUserIdOrderByDateDesc(userId)).thenReturn(Optional.empty());
        when(bloodTestReportRepository.countByUserId(userId)).thenReturn(3L);
        when(fitatuFoodLogRepository.findByUserIdAndDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(healthMetricService.getDailyMetrics(eq(userId), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(aiSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("3");
        assertThat(context).contains("Blood");
    }

    @Test
    void shouldIndicateNoBloodReportsWhenNoneExist() {
        // given
        stubAllEmpty();

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).contains("Blood");
        assertThat(context).contains("no reports");
    }

    // --- page context hint ---

    @Test
    void shouldAppendPageContextHintWhenProvided() {
        // given
        stubAllEmpty();

        // when
        String context = contextBuilder.buildContext("progress", userId);

        // then
        assertThat(context).contains("progress");
        assertThat(context).contains("currently viewing");
    }

    @Test
    void shouldOmitPageContextHintWhenNull() {
        // given
        stubAllEmpty();

        // when
        String context = contextBuilder.buildContext(null, userId);

        // then
        assertThat(context).doesNotContain("currently viewing");
    }

    @Test
    void shouldOmitPageContextHintWhenBlank() {
        // given
        stubAllEmpty();

        // when
        String context = contextBuilder.buildContext("  ", userId);

        // then
        assertThat(context).doesNotContain("currently viewing");
    }
}
