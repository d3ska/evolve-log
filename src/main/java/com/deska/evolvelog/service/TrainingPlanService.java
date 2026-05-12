package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.PlannedExercise;
import com.deska.evolvelog.domain.TrainingBlock;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.dto.request.CreatePlannedExerciseRequest;
import com.deska.evolvelog.dto.request.CreateTrainingPlanRequest;
import com.deska.evolvelog.dto.request.UpdatePlannedExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingPlanRequest;
import com.deska.evolvelog.dto.response.PlanSnapshotEntry;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.PlannedExerciseRepository;
import com.deska.evolvelog.repository.TrainingBlockRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final PlannedExerciseRepository exerciseRepository;
    private final ExerciseDefinitionRepository definitionRepository;
    private final TrainingBlockRepository blockRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TrainingPlan create(User user, CreateTrainingPlanRequest request) {
        TrainingBlock block = resolveBlock(request.blockId(), user.getId());
        TrainingPlan plan = TrainingPlan.builder()
                .user(user)
                .name(request.name())
                .description(request.description())
                .dayOfWeek(request.dayOfWeek())
                .block(block)
                .isActive(true)
                .build();

        if (request.plannedExercises() != null && !request.plannedExercises().isEmpty()) {
            List<PlannedExercise> exercises = buildExercises(request.plannedExercises(), plan);
            plan.getPlannedExercises().addAll(exercises);
        }

        return planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<TrainingPlan> findAll(UUID userId) {
        var result = planRepository.findByUserIdOrderByCreatedAtAsc(userId);
        log.info(result.toString());
        return result;
    }

    @Transactional(readOnly = true)
    public TrainingPlan findById(UUID id, UUID userId) {
        return planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingPlan", id));
    }

    @Transactional
    public TrainingPlan update(UUID id, UUID userId, UpdateTrainingPlanRequest request) {
        TrainingPlan plan = findById(id, userId);
        plan.applyPatch(request.name(), request.description(), request.dayOfWeek(), request.isActive());
        if (request.blockId() != null) {
            TrainingBlock block = resolveBlock(request.blockId().orElse(null), userId);
            plan.setBlock(block);
        }
        if (request.plannedExercises() != null) {
            exerciseRepository.deleteAllByTrainingPlanId(plan.getId());
            List<PlannedExercise> newExercises = buildExercises(request.plannedExercises(), plan);
            plan.getPlannedExercises().clear();
            plan.getPlannedExercises().addAll(newExercises);
        }
        return planRepository.save(plan);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        TrainingPlan plan = findById(id, userId);
        planRepository.delete(plan);
    }

    @Transactional
    public PlannedExercise addExercise(UUID planId, UUID userId, CreatePlannedExerciseRequest request) {
        TrainingPlan plan = findById(planId, userId);
        int position = request.position() != null
                ? request.position()
                : exerciseRepository.countByTrainingPlanId(planId);

        UUID definitionId = resolveDefinitionId(request.exerciseDefinitionId(), userId);

        PlannedExercise exercise = PlannedExercise.builder()
                .trainingPlan(plan)
                .name(request.name())
                .sets(request.sets())
                .repsMin(request.repsMin())
                .repsMax(request.repsMax())
                .restSeconds(request.restSeconds())
                .position(position)
                .notes(request.notes())
                .exerciseDefinitionId(definitionId)
                .build();

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public PlannedExercise updateExercise(UUID planId, UUID exerciseId, UUID userId,
                                          UpdatePlannedExerciseRequest request) {
        findById(planId, userId);
        PlannedExercise exercise = exerciseRepository.findByIdAndUserId(exerciseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PlannedExercise", exerciseId));

        UUID definitionId = resolveDefinitionId(request.exerciseDefinitionId(), userId);
        exercise.applyPatch(request.name(), request.sets(), request.repsMin(), request.repsMax(),
                request.restSeconds(), request.position(), request.notes(), definitionId);

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public void deleteExercise(UUID planId, UUID exerciseId, UUID userId) {
        findById(planId, userId);
        PlannedExercise exercise = exerciseRepository.findByIdAndUserId(exerciseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PlannedExercise", exerciseId));
        exerciseRepository.delete(exercise);
    }

    @Transactional
    public TrainingPlan syncFromSession(UUID planId, UUID sessionId, UUID userId) {
        WorkoutSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSession", sessionId));

        if (session.getStatus() == WorkoutSessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot sync from an active session — finish the session first");
        }

        TrainingPlan plan = findById(planId, userId);
        Map<UUID, PlanSnapshotEntry> snapshotByPlannedId = parseSnapshotMap(session.getPlanSnapshot());

        exerciseRepository.deleteAllByTrainingPlanId(planId);
        plan.getPlannedExercises().clear();

        List<Exercise> sessionExercises = session.getExercises();
        List<PlannedExercise> newExercises = new ArrayList<>();

        for (Exercise ex : sessionExercises) {
            int[] reps = deriveReps(ex, snapshotByPlannedId);
            newExercises.add(PlannedExercise.builder()
                    .trainingPlan(plan)
                    .name(ex.getName())
                    .sets(ex.getSets() != null ? ex.getSets() : 1)
                    .repsMin(reps[0])
                    .repsMax(reps[1])
                    .position(ex.getPosition() != null ? ex.getPosition() : newExercises.size())
                    .exerciseDefinitionId(ex.getExerciseDefinitionId())
                    .build());
        }

        plan.getPlannedExercises().addAll(newExercises);
        return planRepository.save(plan);
    }

    private int[] deriveReps(Exercise exercise, Map<UUID, PlanSnapshotEntry> snapshotByPlannedId) {
        // Try modal reps from workout_sets
        List<Integer> repsValues = exercise.getWorkoutSets().stream()
                .map(WorkoutSet::getReps)
                .filter(r -> r != null)
                .toList();

        if (!repsValues.isEmpty()) {
            int modal = repsValues.stream()
                    .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
                    .entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(0);
            return new int[]{modal, modal};
        }

        // Fallback: snapshot entry for this exercise
        if (exercise.getPlannedExerciseId() != null) {
            PlanSnapshotEntry entry = snapshotByPlannedId.get(exercise.getPlannedExerciseId());
            if (entry != null) {
                return new int[]{
                        entry.repsMin() != null ? entry.repsMin() : 0,
                        entry.repsMax() != null ? entry.repsMax() : 0
                };
            }
        }

        return new int[]{0, 0};
    }

    private Map<UUID, PlanSnapshotEntry> parseSnapshotMap(String json) {
        if (json == null) {
            return Collections.emptyMap();
        }
        try {
            List<PlanSnapshotEntry> entries = objectMapper.readValue(json, new TypeReference<List<PlanSnapshotEntry>>() {});
            return entries.stream()
                    .filter(e -> e.plannedExerciseId() != null)
                    .collect(Collectors.toMap(PlanSnapshotEntry::plannedExerciseId, e -> e));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<PlannedExercise> buildExercises(List<CreatePlannedExerciseRequest> requests, TrainingPlan plan) {
        List<PlannedExercise> exercises = new ArrayList<>();
        UUID userId = plan.getUser().getId();
        for (int i = 0; i < requests.size(); i++) {
            CreatePlannedExerciseRequest req = requests.get(i);
            UUID definitionId = resolveDefinitionId(req.exerciseDefinitionId(), userId);
            exercises.add(PlannedExercise.builder()
                    .trainingPlan(plan)
                    .name(req.name())
                    .sets(req.sets())
                    .repsMin(req.repsMin())
                    .repsMax(req.repsMax())
                    .restSeconds(req.restSeconds())
                    .position(req.position() != null ? req.position() : i)
                    .notes(req.notes())
                    .exerciseDefinitionId(definitionId)
                    .build());
        }
        return exercises;
    }

    private UUID resolveDefinitionId(UUID requestedId, UUID userId) {
        if (requestedId == null) {
            return null;
        }
        definitionRepository.findByIdAccessibleToUser(requestedId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ExerciseDefinition", requestedId));
        return requestedId;
    }

    private TrainingBlock resolveBlock(UUID blockId, UUID userId) {
        if (blockId == null) {
            return null;
        }
        return blockRepository.findByIdAndUserId(blockId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingBlock", blockId));
    }

}
