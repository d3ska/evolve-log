package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.PlannedExercise;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.FinishedSessionDto;
import com.deska.evolvelog.dto.response.MuscleGroupVolumeDto;
import com.deska.evolvelog.dto.response.PlanSnapshotEntry;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionFlowServiceTest {

    @Mock
    private TrainingPlanRepository planRepository;

    @Mock
    private WorkoutSessionRepository sessionRepository;

    @Mock
    private ExerciseDefinitionRepository definitionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private TrainingVolumeService volumeService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WorkoutSessionFlowService service;

    private UUID userId;
    private UUID planId;
    private UUID sessionId;
    private User user;
    private TrainingPlan plan;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        planId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("user@test.com")
                .build();

        plan = TrainingPlan.builder()
                .id(planId)
                .user(user)
                .name("Push Day")
                .build();
    }

    // ── startFromPlan ─────────────────────────────────────────────────────────

    @Test
    void shouldStartFromValidPlanCreatesSessionWithNullActuals() {
        // given
        PlannedExercise pe = PlannedExercise.builder()
                .id(UUID.randomUUID())
                .trainingPlan(plan)
                .name("Bench Press")
                .sets(3)
                .repsMin(8)
                .repsMax(12)
                .position(0)
                .build();
        TrainingPlan planWithExercises = TrainingPlan.builder()
                .id(planId)
                .user(user)
                .name("Push Day")
                .plannedExercises(List.of(pe))
                .build();

        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.of(planWithExercises));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        WorkoutSession result = service.startFromPlan(user, planId);

        // then
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getFinishedAt()).isNull();
        assertThat(result.getDate()).isNotNull();
        assertThat(result.getExercises()).hasSize(1);
        assertThat(result.getExercises().get(0).getName()).isEqualTo("Bench Press");
        assertThat(result.getExercises().get(0).getSets()).isEqualTo(3);
        assertThat(result.getExercises().get(0).getReps()).isNull();
        assertThat(result.getExercises().get(0).getWeightKg()).isNull();
    }

    @Test
    void shouldPropagateDefinitionIdAndPrimaryMuscleOnStart() {
        // given
        UUID definitionId = UUID.randomUUID();
        PlannedExercise pe = PlannedExercise.builder()
                .id(UUID.randomUUID())
                .trainingPlan(plan)
                .name("Squat")
                .sets(4)
                .repsMin(5)
                .repsMax(8)
                .position(0)
                .exerciseDefinitionId(definitionId)
                .build();
        TrainingPlan planWithExercises = TrainingPlan.builder()
                .id(planId)
                .user(user)
                .name("Leg Day")
                .plannedExercises(List.of(pe))
                .build();
        ExerciseDefinition def = ExerciseDefinition.builder()
                .id(definitionId)
                .name("Squat")
                .primaryMuscle("Quadriceps")
                .isSystem(true)
                .build();

        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.of(planWithExercises));
        when(definitionRepository.findAllById(List.of(definitionId))).thenReturn(List.of(def));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        WorkoutSession result = service.startFromPlan(user, planId);

        // then
        assertThat(result.getExercises()).hasSize(1);
        assertThat(result.getExercises().get(0).getExerciseDefinitionId()).isEqualTo(definitionId);
        assertThat(result.getExercises().get(0).getPrimaryMuscle()).isEqualTo("Quadriceps");
    }

    @Test
    void shouldThrowNotFoundWhenStartWithUnknownPlan() {
        // given
        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.startFromPlan(user, planId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowNotFoundWhenStartWithAnotherUsersPlan() {
        // given — plan repository returns empty when userId doesn't match
        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.startFromPlan(user, planId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldSetPlanSnapshotAndPlannedExerciseIdOnStart() throws Exception {
        // given
        UUID peId = UUID.randomUUID();
        PlannedExercise pe = PlannedExercise.builder()
                .id(peId)
                .trainingPlan(plan)
                .name("Bench Press")
                .sets(3)
                .repsMin(8)
                .repsMax(12)
                .restSeconds(90)
                .position(0)
                .build();
        TrainingPlan planWithExercises = TrainingPlan.builder()
                .id(planId).user(user).name("Push Day")
                .plannedExercises(List.of(pe)).build();

        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.of(planWithExercises));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        WorkoutSession result = service.startFromPlan(user, planId);

        // then — snapshot is set and contains the planned exercise
        assertThat(result.getPlanSnapshot()).isNotNull();
        List<PlanSnapshotEntry> entries = new ObjectMapper().readValue(
                result.getPlanSnapshot(), new TypeReference<>() {});
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).plannedExerciseId()).isEqualTo(peId);
        assertThat(entries.get(0).name()).isEqualTo("Bench Press");
        assertThat(entries.get(0).sets()).isEqualTo(3);
        assertThat(entries.get(0).repsMin()).isEqualTo(8);
        assertThat(entries.get(0).repsMax()).isEqualTo(12);

        // then — exercise has plannedExerciseId set
        assertThat(result.getExercises()).hasSize(1);
        assertThat(result.getExercises().get(0).getPlannedExerciseId()).isEqualTo(peId);
    }

    // ── finishSession ─────────────────────────────────────────────────────────

    @Test
    void shouldSetFinishedAtAndComputeDuration() {
        // given
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(45);
        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId)
                .user(user)
                .date(startedAt)
                .startedAt(startedAt)
                .build();

        // no exercises → computeVolumeSummary returns empty DTO without calling volumeService
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        FinishedSessionDto result = service.finishSession(sessionId, userId);

        // then
        assertThat(result.session().finishedAt()).isNotNull();
        assertThat(result.session().durationMinutes()).isGreaterThanOrEqualTo(44);
        assertThat(result.volume().totalVolumeLoad()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.volume().exerciseCount()).isZero();
    }

    @Test
    void shouldBeIdempotentWhenSessionAlreadyFinished() {
        // given
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(60);
        LocalDateTime finishedAt = LocalDateTime.now().minusMinutes(10);
        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId)
                .user(user)
                .date(startedAt)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .durationMinutes(50)
                .build();

        // no exercises → computeVolumeSummary returns empty DTO without calling volumeService
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        // when
        FinishedSessionDto result = service.finishSession(sessionId, userId);

        // then — finishedAt unchanged, save NOT called
        assertThat(result.session().finishedAt()).isEqualTo(finishedAt);
        assertThat(result.session().durationMinutes()).isEqualTo(50);
    }

    @Test
    void shouldThrowNotFoundWhenFinishAnotherUsersSession() {
        // given
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.finishSession(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
