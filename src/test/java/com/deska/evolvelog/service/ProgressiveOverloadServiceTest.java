package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.OverloadHistoryEntryDto;
import com.deska.evolvelog.dto.response.ProgressiveOverloadDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressiveOverloadServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseDefinitionRepository definitionRepository;

    @InjectMocks
    private ProgressiveOverloadService service;

    private UUID userId;
    private UUID definitionId;
    private ExerciseDefinition definition;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        definitionId = UUID.randomUUID();
        definition = ExerciseDefinition.builder()
                .id(definitionId).name("Barbell Bench Press").isSystem(true).build();
    }

    // ── 404 scenarios ────────────────────────────────────────────────────────

    @Test
    void shouldReturn404WhenDefinitionNotFound() {
        // given
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.empty());
        // when / then
        assertThatThrownBy(() -> service.getProgressiveOverload(userId, definitionId, 12))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn404WhenDefinitionBelongsToAnotherUser() {
        // given — user-defined definition owned by someone else
        UUID otherUserId = UUID.randomUUID();
        ExerciseDefinition userDef = ExerciseDefinition.builder()
                .id(definitionId).name("Custom Lift").isSystem(false).userId(otherUserId).build();
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(userDef));
        // when / then
        assertThatThrownBy(() -> service.getProgressiveOverload(userId, definitionId, 12))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── empty history ────────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyHistoryWhenNoSessions() {
        // given
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));
        when(exerciseRepository.findByUserIdAndDefinitionId(eq(userId), eq(definitionId), any(Pageable.class)))
                .thenReturn(List.of());
        // when
        ProgressiveOverloadDto result = service.getProgressiveOverload(userId, definitionId, 12);
        // then
        assertThat(result.history()).isEmpty();
    }

    // ── PR detection ─────────────────────────────────────────────────────────

    @Test
    void shouldMarkFirstSessionAsPr() {
        // given
        Exercise e = exerciseAt(LocalDateTime.now().minusDays(7), 3, 8, "100");
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));
        when(exerciseRepository.findByUserIdAndDefinitionId(eq(userId), eq(definitionId), any(Pageable.class)))
                .thenReturn(List.of(e));
        // when
        ProgressiveOverloadDto result = service.getProgressiveOverload(userId, definitionId, 12);
        // then
        assertThat(result.history()).hasSize(1);
        assertThat(result.history().get(0).isPR()).isTrue();
    }

    @Test
    void shouldMarkNewPrWhenE1RmExceedsPrior() {
        // given: older session with lower weight, newer with higher
        Exercise older = exerciseAt(LocalDateTime.now().minusDays(14), 3, 5, "100"); // e1RM ~116.67
        Exercise newer = exerciseAt(LocalDateTime.now().minusDays(7), 3, 5, "105"); // e1RM ~122.5
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));
        when(exerciseRepository.findByUserIdAndDefinitionId(eq(userId), eq(definitionId), any(Pageable.class)))
                .thenReturn(List.of(newer, older)); // DESC from repo

        // when
        ProgressiveOverloadDto result = service.getProgressiveOverload(userId, definitionId, 12);

        // then: most-recent-first in response
        List<OverloadHistoryEntryDto> history = result.history();
        assertThat(history).hasSize(2);
        // newer is at index 0 (most recent), older at index 1
        assertThat(history.get(0).isPR()).isTrue();  // newer = PR
        assertThat(history.get(1).isPR()).isTrue();  // oldest = always PR
    }

    @Test
    void shouldNotMarkPrWhenE1RmTiesWithPrior() {
        // given: two sessions same weight/reps
        Exercise older = exerciseAt(LocalDateTime.now().minusDays(14), 3, 5, "100");
        Exercise newer = exerciseAt(LocalDateTime.now().minusDays(7), 3, 5, "100"); // same → not PR
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));
        when(exerciseRepository.findByUserIdAndDefinitionId(eq(userId), eq(definitionId), any(Pageable.class)))
                .thenReturn(List.of(newer, older)); // DESC from repo

        // when
        ProgressiveOverloadDto result = service.getProgressiveOverload(userId, definitionId, 12);

        // then
        List<OverloadHistoryEntryDto> history = result.history();
        assertThat(history.get(0).isPR()).isFalse(); // newer = tie, not PR
        assertThat(history.get(1).isPR()).isTrue();  // oldest = always PR
    }

    // ── volumeDelta ───────────────────────────────────────────────────────────

    @Test
    void shouldSetVolumeDeltaNullForOldestEntry() {
        // given: two sessions
        Exercise older = exerciseAt(LocalDateTime.now().minusDays(14), 3, 8, "100"); // VL = 2400
        Exercise newer = exerciseAt(LocalDateTime.now().minusDays(7), 3, 10, "100"); // VL = 3000
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));
        when(exerciseRepository.findByUserIdAndDefinitionId(eq(userId), eq(definitionId), any(Pageable.class)))
                .thenReturn(List.of(newer, older));

        // when
        ProgressiveOverloadDto result = service.getProgressiveOverload(userId, definitionId, 12);

        // then: most-recent-first; index 1 is the oldest → null delta
        List<OverloadHistoryEntryDto> history = result.history();
        assertThat(history.get(0).volumeDelta()).isEqualByComparingTo(new BigDecimal("600")); // 3000 - 2400
        assertThat(history.get(1).volumeDelta()).isNull();
    }

    @Test
    void shouldReturnNegativeVolumeDeltaWhenVolumeDecreased() {
        // given
        Exercise older = exerciseAt(LocalDateTime.now().minusDays(14), 3, 10, "100"); // VL = 3000
        Exercise newer = exerciseAt(LocalDateTime.now().minusDays(7), 3, 8, "100");   // VL = 2400
        when(definitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));
        when(exerciseRepository.findByUserIdAndDefinitionId(eq(userId), eq(definitionId), any(Pageable.class)))
                .thenReturn(List.of(newer, older));

        // when
        ProgressiveOverloadDto result = service.getProgressiveOverload(userId, definitionId, 12);

        // then
        assertThat(result.history().get(0).volumeDelta()).isEqualByComparingTo(new BigDecimal("-600"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Exercise exerciseAt(LocalDateTime date, int sets, int reps, String weight) {
        WorkoutSession session = WorkoutSession.builder()
                .id(UUID.randomUUID()).date(date).build();
        return Exercise.builder()
                .workoutSession(session)
                .sets(sets).reps(reps)
                .weightKg(new BigDecimal(weight))
                .exerciseDefinitionId(definitionId)
                .build();
    }
}
