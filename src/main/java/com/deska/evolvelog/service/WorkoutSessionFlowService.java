package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.PlannedExercise;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.dto.response.FinishedSessionDto;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutSessionFlowService {

    private final TrainingPlanRepository planRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseDefinitionRepository definitionRepository;
    private final ExerciseRepository exerciseRepository;
    private final TrainingVolumeService volumeService;

    @Transactional
    public WorkoutSession startFromPlan(User user, UUID trainingPlanId) {
        if (sessionRepository.existsByUserIdAndStatus(user.getId(), WorkoutSessionStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an active workout session");
        }

        TrainingPlan plan = planRepository.findByIdAndUserId(trainingPlanId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TrainingPlan", trainingPlanId));

        LocalDateTime now = LocalDateTime.now();

        WorkoutSession session = WorkoutSession.builder()
                .user(user)
                .trainingPlan(plan)
                .date(now)
                .startedAt(now)
                .build();
        session.activate();

        List<PlannedExercise> plannedExercises = plan.getPlannedExercises();
        if (!plannedExercises.isEmpty()) {
            Map<UUID, ExerciseDefinition> definitions = loadDefinitions(plannedExercises);
            List<Exercise> exercises = new ArrayList<>();
            for (PlannedExercise pe : plannedExercises) {
                ExerciseDefinition def = pe.getExerciseDefinitionId() != null
                        ? definitions.get(pe.getExerciseDefinitionId())
                        : null;
                Exercise exercise = Exercise.builder()
                        .workoutSession(session)
                        .name(pe.getName())
                        .sets(pe.getSets())
                        .reps(null)
                        .weightKg(null)
                        .position(pe.getPosition())
                        .exerciseDefinitionId(pe.getExerciseDefinitionId())
                        .primaryMuscle(def != null ? def.getPrimaryMuscle() : null)
                        .build();
                for (int i = 1; i <= pe.getSets(); i++) {
                    exercise.getWorkoutSets().add(WorkoutSet.builder()
                            .exercise(exercise)
                            .setNumber(i)
                            .build());
                }
                exercises.add(exercise);
            }
            session.getExercises().addAll(exercises);
        }

        return sessionRepository.save(session);
    }

    @Transactional
    public FinishedSessionDto finishSession(UUID sessionId, UUID userId) {
        WorkoutSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSession", sessionId));

        if (session.getFinishedAt() == null) {
            LocalDateTime finishedAt = LocalDateTime.now();
            Integer duration = session.getStartedAt() != null
                    ? (int) ChronoUnit.MINUTES.between(session.getStartedAt(), finishedAt)
                    : session.getDurationMinutes();
            session.finish(finishedAt, duration);
            session = sessionRepository.save(session);
        }

        SessionVolumeSummaryDto volume = computeVolumeSummary(session, userId);
        return new FinishedSessionDto(WorkoutSessionDto.from(session), volume);
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSession> getActiveSession(UUID userId) {
        return sessionRepository.findByUserIdAndStatus(userId, WorkoutSessionStatus.ACTIVE);
    }

    @Transactional
    public Exercise addExercise(UUID sessionId, UUID userId, String name, Integer sets) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSession", sessionId));

        int position = exerciseRepository.countByWorkoutSessionId(sessionId);

        Exercise exercise = Exercise.builder()
                .workoutSession(sessionRepository.getReferenceById(sessionId))
                .name(name)
                .sets(sets)
                .position(position)
                .build();

        for (int i = 1; i <= sets; i++) {
            exercise.getWorkoutSets().add(WorkoutSet.builder()
                    .exercise(exercise)
                    .setNumber(i)
                    .build());
        }

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public void removeExercise(UUID exerciseId, UUID userId) {
        Exercise exercise = exerciseRepository.findByIdAndWorkoutSessionUserId(exerciseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));
        exerciseRepository.delete(exercise);
    }

    private Map<UUID, ExerciseDefinition> loadDefinitions(List<PlannedExercise> plannedExercises) {
        List<UUID> ids = plannedExercises.stream()
                .map(PlannedExercise::getExerciseDefinitionId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return definitionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ExerciseDefinition::getId, Function.identity()));
    }

    private SessionVolumeSummaryDto computeVolumeSummary(WorkoutSession session, UUID userId) {
        List<Exercise> exercises = session.getExercises();
        if (exercises.isEmpty()) {
            return new SessionVolumeSummaryDto(session.getId(), BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0, List.of());
        }
        boolean hasNullReps = exercises.stream().anyMatch(e -> e.getReps() == null);
        if (hasNullReps) {
            return new SessionVolumeSummaryDto(session.getId(), BigDecimal.ZERO, BigDecimal.ZERO, 0.0, exercises.size(), List.of());
        }
        return volumeService.getSessionVolumeSummary(session.getId(), userId);
    }
}
