package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.PlannedExercise;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreatePlannedExerciseRequest;
import com.deska.evolvelog.dto.request.CreateTrainingPlanRequest;
import com.deska.evolvelog.dto.request.UpdatePlannedExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingPlanRequest;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.PlannedExerciseRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final PlannedExerciseRepository exerciseRepository;
    private final ExerciseDefinitionRepository definitionRepository;

    @Transactional
    public TrainingPlan create(User user, CreateTrainingPlanRequest request) {
        TrainingPlan plan = TrainingPlan.builder()
                .user(user)
                .name(request.name())
                .description(request.description())
                .dayOfWeek(request.dayOfWeek())
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
}
