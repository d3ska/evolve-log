package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.TrainingBlock;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.dto.request.CreateTrainingPlanRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingPlanRequest;
import com.deska.evolvelog.dto.response.PlanSnapshotEntry;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.PlannedExerciseRepository;
import com.deska.evolvelog.repository.TrainingBlockRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    @Mock TrainingPlanRepository planRepository;
    @Mock PlannedExerciseRepository exerciseRepository;
    @Mock ExerciseDefinitionRepository definitionRepository;
    @Mock TrainingBlockRepository blockRepository;
    @Mock WorkoutSessionRepository sessionRepository;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    TrainingPlanService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private TrainingBlock buildBlock(UUID userId) {
        return TrainingBlock.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Block")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private TrainingPlan buildPlan(User planUser, TrainingBlock block) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID())
                .user(planUser)
                .name("Plan")
                .isActive(true)
                .block(block)
                .build();
    }

    @Test
    void create_withValidBlockId_setsBlockOnPlan() {
        var block = buildBlock(user.getId());
        var req = new CreateTrainingPlanRequest("Plan", null, null, block.getId(), null);

        when(blockRepository.findByIdAndUserId(block.getId(), user.getId())).thenReturn(Optional.of(block));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.create(user, req);

        assert saved.getBlockId().equals(block.getId());
        verify(blockRepository).findByIdAndUserId(block.getId(), user.getId());
    }

    @Test
    void create_withBlockIdOfAnotherUser_throws404() {
        UUID foreignBlockId = UUID.randomUUID();
        var req = new CreateTrainingPlanRequest("Plan", null, null, foreignBlockId, null);

        when(blockRepository.findByIdAndUserId(foreignBlockId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(user, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withExplicitNullBlockId_clearsFk() {
        var block = buildBlock(user.getId());
        var plan = buildPlan(user, block);

        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new UpdateTrainingPlanRequest(null, null, null, null, Optional.empty(), null);
        var updated = service.update(plan.getId(), user.getId(), req);

        assert updated.getBlockId() == null;
        verify(planRepository).save(plan);
    }

    @Test
    void update_withBlockIdOfAnotherUser_throws404() {
        var plan = buildPlan(user, null);
        UUID foreignBlockId = UUID.randomUUID();

        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(blockRepository.findByIdAndUserId(foreignBlockId, user.getId())).thenReturn(Optional.empty());

        var req = new UpdateTrainingPlanRequest(null, null, null, null, Optional.of(foreignBlockId), null);

        assertThatThrownBy(() -> service.update(plan.getId(), user.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── syncFromSession ───────────────────────────────────────────────────────

    @Test
    void syncFromSession_throws409_whenSessionIsActive() {
        UUID sessionId = UUID.randomUUID();
        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user).status(WorkoutSessionStatus.ACTIVE).build();

        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.of(session));

        var plan = buildPlan(user, null);

        assertThatThrownBy(() -> service.syncFromSession(plan.getId(), sessionId, user.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active session");
    }

    @Test
    void syncFromSession_throws404_whenSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncFromSession(planId, sessionId, user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void syncFromSession_derivesRepsFromModalWorkoutSets() throws Exception {
        UUID sessionId = UUID.randomUUID();
        WorkoutSet ws1 = WorkoutSet.builder().setNumber(1).reps(8).build();
        WorkoutSet ws2 = WorkoutSet.builder().setNumber(2).reps(8).build();
        WorkoutSet ws3 = WorkoutSet.builder().setNumber(3).reps(10).build();
        Exercise ex = Exercise.builder()
                .name("Bench Press").sets(3).position(0)
                .workoutSets(List.of(ws1, ws2, ws3)).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user)
                .exercises(List.of(ex)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.of(session));

        var plan = buildPlan(user, null);
        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrainingPlan result = service.syncFromSession(plan.getId(), sessionId, user.getId());

        assertThat(result.getPlannedExercises()).hasSize(1);
        var planned = result.getPlannedExercises().get(0);
        assertThat(planned.getName()).isEqualTo("Bench Press");
        assertThat(planned.getRepsMin()).isEqualTo(8);
        assertThat(planned.getRepsMax()).isEqualTo(8);
    }

    @Test
    void syncFromSession_fallsBackToSnapshotReps_whenNoWorkoutSets() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID peId = UUID.randomUUID();

        String snapshotJson = new ObjectMapper().writeValueAsString(List.of(
                new PlanSnapshotEntry(peId, "Squat", 4, 5, 8, 120, 0)
        ));
        Exercise ex = Exercise.builder()
                .name("Squat").sets(4).position(0)
                .plannedExerciseId(peId).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user).planSnapshot(snapshotJson)
                .exercises(List.of(ex)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.of(session));

        var plan = buildPlan(user, null);
        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrainingPlan result = service.syncFromSession(plan.getId(), sessionId, user.getId());

        assertThat(result.getPlannedExercises()).hasSize(1);
        var planned = result.getPlannedExercises().get(0);
        assertThat(planned.getRepsMin()).isEqualTo(5);
        assertThat(planned.getRepsMax()).isEqualTo(8);
    }

    @Test
    void syncFromSession_fallsBackToZeroReps_whenNoSetsAndNoSnapshot() {
        UUID sessionId = UUID.randomUUID();
        Exercise ex = Exercise.builder()
                .name("Cable Fly").sets(3).position(0).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user)
                .exercises(List.of(ex)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.of(session));

        var plan = buildPlan(user, null);
        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrainingPlan result = service.syncFromSession(plan.getId(), sessionId, user.getId());

        assertThat(result.getPlannedExercises()).hasSize(1);
        var planned = result.getPlannedExercises().get(0);
        assertThat(planned.getRepsMin()).isZero();
        assertThat(planned.getRepsMax()).isZero();
    }
}
