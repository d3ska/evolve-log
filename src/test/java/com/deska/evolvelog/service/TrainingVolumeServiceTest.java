package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.WeeklyVolumeRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingVolumeServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private TrainingVolumeService service;

    private UUID userId;
    private UUID sessionId;
    private WorkoutSession session;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        session = WorkoutSession.builder()
                .id(sessionId)
                .date(LocalDateTime.now())
                .build();
    }

    // ── getSessionVolumeSummary ──────────────────────────────────────────────

    @Test
    void shouldReturnNullWhenSessionNotFound() {
        // given
        when(exerciseRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(List.of());
        // when / then
        assertThatThrownBy(() -> service.getSessionVolumeSummary(sessionId, userId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldCalculateFullSessionVolume() {
        // given — two exercises with weight and RPE
        Exercise e1 = Exercise.builder()
                .workoutSession(session).sets(3).reps(8).weightKg(new BigDecimal("100"))
                .rpe(new BigDecimal("8.0")).primaryMuscle("chest").build();
        Exercise e2 = Exercise.builder()
                .workoutSession(session).sets(4).reps(10).weightKg(new BigDecimal("60"))
                .rpe(new BigDecimal("7.0")).primaryMuscle("back").build();
        when(exerciseRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(List.of(e1, e2));

        // when
        SessionVolumeSummaryDto result = service.getSessionVolumeSummary(sessionId, userId);

        // then: e1 VL = 3*8*100 = 2400, e2 VL = 4*10*60 = 2400, total = 4800
        assertThat(result.totalVolumeLoad()).isEqualByComparingTo(new BigDecimal("4800"));
        assertThat(result.exerciseCount()).isEqualTo(2);
        assertThat(result.rpeCompleteness()).isEqualTo(1.0);
        assertThat(result.byMuscleGroup()).hasSize(2);
    }

    @Test
    void shouldExcludeBodyweightExercisesFromVolumeButCountThem() {
        // given — one bodyweight (no weight), one weighted
        Exercise bodyweight = Exercise.builder()
                .workoutSession(session).sets(3).reps(15).weightKg(null)
                .primaryMuscle("core").build();
        Exercise weighted = Exercise.builder()
                .workoutSession(session).sets(3).reps(8).weightKg(new BigDecimal("80"))
                .primaryMuscle("chest").build();
        when(exerciseRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(List.of(bodyweight, weighted));

        // when
        SessionVolumeSummaryDto result = service.getSessionVolumeSummary(sessionId, userId);

        // then: only weighted counts in volume; bodyweight still in exerciseCount
        assertThat(result.totalVolumeLoad()).isEqualByComparingTo(new BigDecimal("1920")); // 3*8*80
        assertThat(result.exerciseCount()).isEqualTo(2);
        assertThat(result.byMuscleGroup()).hasSize(1); // only chest (core has no volume)
    }

    @Test
    void shouldComputePartialRpeCompleteness() {
        // given — one with RPE, one without
        Exercise withRpe = Exercise.builder()
                .workoutSession(session).sets(3).reps(8).weightKg(new BigDecimal("100"))
                .rpe(new BigDecimal("8.0")).primaryMuscle("chest").build();
        Exercise noRpe = Exercise.builder()
                .workoutSession(session).sets(3).reps(10).weightKg(new BigDecimal("60"))
                .primaryMuscle("back").build();
        when(exerciseRepository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(List.of(withRpe, noRpe));

        // when
        SessionVolumeSummaryDto result = service.getSessionVolumeSummary(sessionId, userId);

        // then
        assertThat(result.rpeCompleteness()).isEqualTo(0.5);
        assertThat(result.totalInternalLoad()).isNotNull(); // partial — has some IL
    }

    // ── getWeeklyVolumeByMuscle ──────────────────────────────────────────────

    @Test
    void shouldThrowWhenDateRangeExceedsFiftyTwoWeeks() {
        // given
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2025, 2, 1); // > 52 weeks

        // when / then
        assertThatThrownBy(() -> service.getWeeklyVolumeByMuscle(userId, from, to, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnWeeklyVolumeForValidRange() {
        // given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 3, 31);
        WeeklyVolumeRow row = mockRow(LocalDate.of(2025, 1, 6), "chest", new BigDecimal("2400"), 2L);
        when(exerciseRepository.findWeeklyVolumeByMuscle(eq(userId), any(), any(), isNull()))
                .thenReturn(List.of(row));

        // when
        var result = service.getWeeklyVolumeByMuscle(userId, from, to, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).muscle()).isEqualTo("chest");
        assertThat(result.get(0).volumeLoad()).isEqualByComparingTo(new BigDecimal("2400"));
    }

    private WeeklyVolumeRow mockRow(LocalDate weekStart, String muscle, BigDecimal volumeLoad, long sessionCount) {
        return new WeeklyVolumeRow() {
            public LocalDate getWeekStart() { return weekStart; }
            public String getMuscle() { return muscle; }
            public BigDecimal getVolumeLoad() { return volumeLoad; }
            public Long getSessionCount() { return sessionCount; }
        };
    }
}
