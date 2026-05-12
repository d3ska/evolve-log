package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.PlanSnapshotEntry;
import com.deska.evolvelog.dto.response.SessionDeviationDto;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutDeviationServiceTest {

    @Mock
    WorkoutSessionRepository sessionRepository;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    WorkoutDeviationService service;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private User user;
    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@example.com").build();
    }

    @Test
    void getDeviations_unsupported_whenNoPlanSnapshot() {
        WorkoutSession session = WorkoutSession.builder().id(sessionId).user(user).build();
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        SessionDeviationDto result = service.getDeviations(sessionId, userId);

        assertThat(result.supported()).isFalse();
        assertThat(result.entries()).isEmpty();
    }

    @Test
    void getDeviations_throwsNotFound_whenSessionBelongsToOtherUser() {
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeviations(sessionId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDeviations_allCompleted_whenAllPlannedExercisesPerformed() throws Exception {
        UUID pe1 = UUID.randomUUID();
        UUID pe2 = UUID.randomUUID();
        UUID ex1Id = UUID.randomUUID();
        UUID ex2Id = UUID.randomUUID();

        String snapshot = MAPPER.writeValueAsString(List.of(
                new PlanSnapshotEntry(pe1, "Bench Press", 3, 8, 12, 90, 0),
                new PlanSnapshotEntry(pe2, "Squat", 4, 5, 8, 120, 1)
        ));

        Exercise ex1 = Exercise.builder().id(ex1Id).name("Bench Press").sets(3).position(0).plannedExerciseId(pe1).build();
        Exercise ex2 = Exercise.builder().id(ex2Id).name("Squat").sets(4).position(1).plannedExerciseId(pe2).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user).planSnapshot(snapshot)
                .exercises(List.of(ex1, ex2)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        SessionDeviationDto result = service.getDeviations(sessionId, userId);

        assertThat(result.supported()).isTrue();
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries()).allMatch(e -> e.status().equals("COMPLETED"));
    }

    @Test
    void getDeviations_skipped_whenPlannedExerciseNotPerformed() throws Exception {
        UUID pe1 = UUID.randomUUID();
        UUID pe2 = UUID.randomUUID();
        UUID ex1Id = UUID.randomUUID();

        String snapshot = MAPPER.writeValueAsString(List.of(
                new PlanSnapshotEntry(pe1, "Bench Press", 3, 8, 12, 90, 0),
                new PlanSnapshotEntry(pe2, "Skipped Exercise", 3, 10, 15, 60, 1)
        ));

        Exercise ex1 = Exercise.builder().id(ex1Id).name("Bench Press").sets(3).position(0).plannedExerciseId(pe1).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user).planSnapshot(snapshot)
                .exercises(List.of(ex1)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        SessionDeviationDto result = service.getDeviations(sessionId, userId);

        assertThat(result.supported()).isTrue();
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries()).anySatisfy(e -> {
            assertThat(e.status()).isEqualTo("COMPLETED");
            assertThat(e.name()).isEqualTo("Bench Press");
        });
        assertThat(result.entries()).anySatisfy(e -> {
            assertThat(e.status()).isEqualTo("SKIPPED");
            assertThat(e.name()).isEqualTo("Skipped Exercise");
            assertThat(e.sessionExerciseId()).isNull();
        });
    }

    @Test
    void getDeviations_added_whenExerciseHasNullPlannedId() throws Exception {
        UUID pe1 = UUID.randomUUID();
        UUID ex1Id = UUID.randomUUID();
        UUID ex2Id = UUID.randomUUID();

        String snapshot = MAPPER.writeValueAsString(List.of(
                new PlanSnapshotEntry(pe1, "Bench Press", 3, 8, 12, 90, 0)
        ));

        Exercise ex1 = Exercise.builder().id(ex1Id).name("Bench Press").sets(3).position(0).plannedExerciseId(pe1).build();
        Exercise ex2 = Exercise.builder().id(ex2Id).name("Curls").sets(3).position(1).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user).planSnapshot(snapshot)
                .exercises(List.of(ex1, ex2)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        SessionDeviationDto result = service.getDeviations(sessionId, userId);

        assertThat(result.supported()).isTrue();
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries()).anySatisfy(e -> assertThat(e.status()).isEqualTo("COMPLETED"));
        assertThat(result.entries()).anySatisfy(e -> {
            assertThat(e.status()).isEqualTo("ADDED");
            assertThat(e.name()).isEqualTo("Curls");
            assertThat(e.plannedExerciseId()).isNull();
            assertThat(e.sessionExerciseId()).isEqualTo(ex2Id);
        });
    }

    @Test
    void getDeviations_cascadeNull_whenPlannedExerciseDeletedAfterSessionStart() throws Exception {
        UUID pe1 = UUID.randomUUID();
        UUID exId = UUID.randomUUID();

        String snapshot = MAPPER.writeValueAsString(List.of(
                new PlanSnapshotEntry(pe1, "Bench Press", 3, 8, 12, 90, 0)
        ));

        // Exercise originally linked to pe1, but ON DELETE SET NULL fired (planned exercise was deleted)
        Exercise ex = Exercise.builder().id(exId).name("Bench Press").sets(3).position(0).build();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId).user(user).planSnapshot(snapshot)
                .exercises(List.of(ex)).build();
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        SessionDeviationDto result = service.getDeviations(sessionId, userId);

        assertThat(result.supported()).isTrue();
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries()).anySatisfy(e -> {
            assertThat(e.status()).isEqualTo("SKIPPED");
            assertThat(e.plannedExerciseId()).isEqualTo(pe1);
        });
        assertThat(result.entries()).anySatisfy(e -> {
            assertThat(e.status()).isEqualTo("ADDED");
            assertThat(e.sessionExerciseId()).isEqualTo(exId);
        });
    }
}
