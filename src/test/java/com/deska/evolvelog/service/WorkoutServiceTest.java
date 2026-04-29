package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.request.CreateExerciseRequest;
import com.deska.evolvelog.dto.request.CreateWorkoutSessionRequest;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.deska.evolvelog.repository.WorkoutSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutSessionRepository sessionRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private TrainingPlanRepository trainingPlanRepository;
    @Mock
    private ExerciseDefinitionRepository definitionRepository;
    @Mock
    private WorkoutSetRepository workoutSetRepository;

    @InjectMocks
    private WorkoutService service;

    private User user;
    private UUID definitionId;
    private ExerciseDefinition benchPressDefinition;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@example.com").build();
        definitionId = UUID.randomUUID();
        benchPressDefinition = ExerciseDefinition.builder()
                .id(definitionId).name("Bench Press").isSystem(true).primaryMuscle("chest").build();
    }

    // ── T7: name matches system definition ───────────────────────────────────

    @Test
    void shouldPopulateDefinitionIdAndPrimaryMuscleWhenNameMatchesSystemDefinition() {
        // given
        CreateExerciseRequest exerciseReq = new CreateExerciseRequest(
                "Bench Press", 3, 8, new BigDecimal("80"), null, null, null, null);
        CreateWorkoutSessionRequest request = new CreateWorkoutSessionRequest(
                LocalDateTime.now().minusHours(1), 60, null, null, List.of(exerciseReq));

        when(definitionRepository.findByNameIgnoreCaseAndIsSystemTrue("Bench Press"))
                .thenReturn(Optional.of(benchPressDefinition));
        when(definitionRepository.findById(definitionId))
                .thenReturn(Optional.of(benchPressDefinition));
        when(sessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        WorkoutSession saved = service.create(user, request);

        // then
        assertThat(saved.getExercises()).hasSize(1);
        Exercise exercise = saved.getExercises().get(0);
        assertThat(exercise.getExerciseDefinitionId()).isEqualTo(definitionId);
        assertThat(exercise.getPrimaryMuscle()).isEqualTo("chest");
    }

    // ── T8: name does not match any definition ────────────────────────────────

    @Test
    void shouldSaveWithNullDefinitionIdWhenNameDoesNotMatchAnySystemDefinition() {
        // given
        CreateExerciseRequest exerciseReq = new CreateExerciseRequest(
                "My Custom Move", 3, 8, new BigDecimal("50"), null, null, null, null);
        CreateWorkoutSessionRequest request = new CreateWorkoutSessionRequest(
                LocalDateTime.now().minusHours(1), 45, null, null, List.of(exerciseReq));

        when(definitionRepository.findByNameIgnoreCaseAndIsSystemTrue("My Custom Move"))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(WorkoutSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when — must not throw
        WorkoutSession saved = service.create(user, request);

        // then
        assertThat(saved.getExercises()).hasSize(1);
        Exercise exercise = saved.getExercises().get(0);
        assertThat(exercise.getExerciseDefinitionId()).isNull();
        assertThat(exercise.getPrimaryMuscle()).isNull();
    }
}
