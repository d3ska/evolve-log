package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.WorkoutSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSetServiceTest {

    @Mock
    private WorkoutSetRepository setRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private WorkoutSetService service;

    private UUID userId;
    private UUID setId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        setId = UUID.randomUUID();
    }

    // ── updateSet partial patch ───────────────────────────────────────────────

    @Test
    void updateSet_shouldUpdateOnlyReps() {
        WorkoutSet ws = WorkoutSet.builder()
                .setNumber(1).reps(8).weightKg(new BigDecimal("60.00")).build();
        when(setRepository.findByIdAndUserId(setId, userId)).thenReturn(Optional.of(ws));
        when(setRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSet result = service.updateSet(setId, userId, 10, null, null);

        assertThat(result.getReps()).isEqualTo(10);
        assertThat(result.getWeightKg()).isEqualByComparingTo("60.00");
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void updateSet_shouldUpdateOnlyWeightKg() {
        WorkoutSet ws = WorkoutSet.builder()
                .setNumber(1).reps(8).weightKg(new BigDecimal("60.00")).build();
        when(setRepository.findByIdAndUserId(setId, userId)).thenReturn(Optional.of(ws));
        when(setRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSet result = service.updateSet(setId, userId, null, new BigDecimal("80.00"), null);

        assertThat(result.getReps()).isEqualTo(8);
        assertThat(result.getWeightKg()).isEqualByComparingTo("80.00");
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void updateSet_shouldUpdateOnlyCompleted() {
        WorkoutSet ws = WorkoutSet.builder()
                .setNumber(1).reps(8).weightKg(new BigDecimal("60.00")).build();
        when(setRepository.findByIdAndUserId(setId, userId)).thenReturn(Optional.of(ws));
        when(setRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSet result = service.updateSet(setId, userId, null, null, true);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getReps()).isEqualTo(8);
        assertThat(result.getWeightKg()).isEqualByComparingTo("60.00");
    }

    @Test
    void updateSet_shouldUpdateMultipleFieldsAtOnce() {
        WorkoutSet ws = WorkoutSet.builder()
                .setNumber(1).reps(8).weightKg(new BigDecimal("60.00")).build();
        when(setRepository.findByIdAndUserId(setId, userId)).thenReturn(Optional.of(ws));
        when(setRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSet result = service.updateSet(setId, userId, 12, new BigDecimal("70.00"), true);

        assertThat(result.getReps()).isEqualTo(12);
        assertThat(result.getWeightKg()).isEqualByComparingTo("70.00");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void updateSet_allNullShouldLeaveValuesUnchanged() {
        WorkoutSet ws = WorkoutSet.builder()
                .setNumber(1).reps(8).weightKg(new BigDecimal("60.00")).build();
        when(setRepository.findByIdAndUserId(setId, userId)).thenReturn(Optional.of(ws));
        when(setRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSet result = service.updateSet(setId, userId, null, null, null);

        assertThat(result.getReps()).isEqualTo(8);
        assertThat(result.getWeightKg()).isEqualByComparingTo("60.00");
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    void updateSet_shouldThrowNotFoundWhenSetBelongsToOtherUser() {
        when(setRepository.findByIdAndUserId(setId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSet(setId, userId, 10, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
