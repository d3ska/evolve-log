package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.UserRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.deska.evolvelog.repository.WorkoutSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActiveWorkoutSessionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    WorkoutSessionFlowService flowService;

    @Autowired
    WorkoutSetService workoutSetService;

    @Autowired
    WorkoutSessionRepository sessionRepository;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Autowired
    WorkoutSetRepository workoutSetRepository;

    @Autowired
    TrainingPlanRepository planRepository;

    @Autowired
    UserRepository userRepository;

    private User user;
    private TrainingPlan plan;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("active-workout-test@example.com")
                .build());

        plan = planRepository.save(TrainingPlan.builder()
                .user(user)
                .name("Test Plan")
                .build());
    }

    // ── T19: Session status transitions ──────────────────────────────────────

    @Test
    void startFromPlan_shouldSetStatusToActive() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());

        assertThat(session.getStatus()).isEqualTo(WorkoutSessionStatus.ACTIVE);
        WorkoutSession persisted = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(WorkoutSessionStatus.ACTIVE);
    }

    @Test
    void finishSession_shouldSetStatusToFinished() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        flowService.finishSession(session.getId(), user.getId());

        WorkoutSession persisted = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(WorkoutSessionStatus.FINISHED);
        assertThat(persisted.getFinishedAt()).isNotNull();
    }

    @Test
    void startFromPlan_shouldThrow409WhenActiveSessionAlreadyExists() {
        flowService.startFromPlan(user, plan.getId());

        assertThatThrownBy(() -> flowService.startFromPlan(user, plan.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active workout session");
    }

    @Test
    void startFromPlan_shouldAllowNewActiveSessionAfterPreviousIsFinished() {
        WorkoutSession first = flowService.startFromPlan(user, plan.getId());
        flowService.finishSession(first.getId(), user.getId());

        WorkoutSession second = flowService.startFromPlan(user, plan.getId());
        assertThat(second.getStatus()).isEqualTo(WorkoutSessionStatus.ACTIVE);
    }

    // ── T20: GET /active ─────────────────────────────────────────────────────

    @Test
    void getActiveSession_shouldReturnActiveSession() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());

        Optional<WorkoutSession> result = flowService.getActiveSession(user.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }

    @Test
    void getActiveSession_shouldReturnEmptyWhenNoActiveSession() {
        Optional<WorkoutSession> result = flowService.getActiveSession(user.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void getActiveSession_shouldReturnEmptyAfterSessionFinished() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        flowService.finishSession(session.getId(), user.getId());

        Optional<WorkoutSession> result = flowService.getActiveSession(user.getId());
        assertThat(result).isEmpty();
    }

    // ── T21: Exercise mutations work on any session status ───────────────────

    @Test
    void addExercise_shouldWorkOnActiveSession() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());

        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Bench Press", 3);

        assertThat(exercise.getId()).isNotNull();
        assertThat(exercise.getName()).isEqualTo("Bench Press");
        assertThat(exercise.getSets()).isEqualTo(3);
    }

    @Test
    void addExercise_shouldWorkOnFinishedSession() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        flowService.finishSession(session.getId(), user.getId());

        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Squat", 4);

        assertThat(exercise.getId()).isNotNull();
    }

    @Test
    void removeExercise_shouldDeleteExerciseRegardlessOfStatus() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Pull-up", 3);

        flowService.removeExercise(exercise.getId(), user.getId());

        assertThat(exerciseRepository.findById(exercise.getId())).isEmpty();
    }

    @Test
    void removeExercise_shouldThrowNotFoundForOtherUser() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Dip", 3);

        java.util.UUID otherId = java.util.UUID.randomUUID();
        assertThatThrownBy(() -> flowService.removeExercise(exercise.getId(), otherId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── T22: Set CRUD ─────────────────────────────────────────────────────────

    @Test
    void addSet_shouldPersistSet() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Deadlift", 1);

        WorkoutSet ws = workoutSetService.addSet(exercise.getId(), user.getId(), 1, 5, new BigDecimal("100.00"));

        assertThat(ws.getId()).isNotNull();
        assertThat(ws.getReps()).isEqualTo(5);
        assertThat(ws.getWeightKg()).isEqualByComparingTo("100.00");
        assertThat(ws.isCompleted()).isFalse();
    }

    @Test
    void updateSet_shouldApplyPartialPatch() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Overhead Press", 1);
        WorkoutSet ws = workoutSetService.addSet(exercise.getId(), user.getId(), 1, 8, new BigDecimal("60.00"));

        WorkoutSet updated = workoutSetService.updateSet(ws.getId(), user.getId(), 10, null, true);

        assertThat(updated.getReps()).isEqualTo(10);
        assertThat(updated.getWeightKg()).isEqualByComparingTo("60.00");
        assertThat(updated.isCompleted()).isTrue();
    }

    @Test
    void deleteSet_shouldRemoveSetFromDatabase() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Row", 1);
        WorkoutSet ws = workoutSetService.addSet(exercise.getId(), user.getId(), 1, 10, new BigDecimal("80.00"));

        workoutSetService.deleteSet(ws.getId(), user.getId());

        assertThat(workoutSetRepository.findById(ws.getId())).isEmpty();
    }

    @Test
    void setCrud_shouldThrowNotFoundWhenAccessedByOtherUser() {
        WorkoutSession session = flowService.startFromPlan(user, plan.getId());
        Exercise exercise = flowService.addExercise(session.getId(), user.getId(), "Curl", 1);
        WorkoutSet ws = workoutSetService.addSet(exercise.getId(), user.getId(), 1, 12, new BigDecimal("20.00"));

        java.util.UUID otherId = java.util.UUID.randomUUID();
        assertThatThrownBy(() -> workoutSetService.updateSet(ws.getId(), otherId, 10, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> workoutSetService.deleteSet(ws.getId(), otherId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
