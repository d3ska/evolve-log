package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.dto.request.CreateExerciseRequest;
import com.deska.evolvelog.dto.request.CreateWorkoutSessionRequest;
import com.deska.evolvelog.dto.request.UpdateExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateWorkoutSessionRequest;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.deska.evolvelog.repository.WorkoutSetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WorkoutService {

    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final ExerciseDefinitionRepository definitionRepository;
    private final WorkoutSetRepository workoutSetRepository;

    public WorkoutService(WorkoutSessionRepository sessionRepository,
                          ExerciseRepository exerciseRepository,
                          TrainingPlanRepository trainingPlanRepository,
                          ExerciseDefinitionRepository definitionRepository,
                          WorkoutSetRepository workoutSetRepository) {
        this.sessionRepository = sessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.definitionRepository = definitionRepository;
        this.workoutSetRepository = workoutSetRepository;
    }

    @Transactional
    public WorkoutSession create(User user, CreateWorkoutSessionRequest request) {
        TrainingPlan plan = resolveTrainingPlan(request.trainingPlanId(), user.getId());

        WorkoutSession session = WorkoutSession.builder()
                .user(user)
                .date(request.date())
                .durationMinutes(request.durationMinutes())
                .notes(request.notes())
                .trainingPlan(plan)
                .build();

        if (request.exercises() != null && !request.exercises().isEmpty()) {
            List<Exercise> exercises = buildExercises(request.exercises(), session);
            session.getExercises().addAll(exercises);
        }

        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSessionDto> findAll(UUID userId, int page, int size) {
        return sessionRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(page, size))
                .map(WorkoutSessionDto::from);
    }

    @Transactional(readOnly = true)
    public WorkoutSession findById(UUID id, UUID userId) {
        return sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSession", id));
    }

    @Transactional
    public WorkoutSession update(UUID id, UUID userId, UpdateWorkoutSessionRequest request) {
        WorkoutSession session = findById(id, userId);
        TrainingPlan plan = request.trainingPlanId() != null
                ? resolveTrainingPlan(request.trainingPlanId(), userId)
                : null;
        session.applyPatch(request.date(), request.durationMinutes(), request.notes(), plan);
        return sessionRepository.save(session);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        WorkoutSession session = findById(id, userId);
        sessionRepository.delete(session);
    }

    @Transactional
    public Exercise addExercise(UUID sessionId, UUID userId, CreateExerciseRequest request) {
        WorkoutSession session = findById(sessionId, userId);
        int position = request.position() != null
                ? request.position()
                : exerciseRepository.countByWorkoutSessionId(sessionId);

        String primaryMuscle = resolvePrimaryMuscle(request.exerciseDefinitionId());

        Exercise exercise = Exercise.builder()
                .workoutSession(session)
                .name(request.name())
                .sets(request.sets())
                .reps(request.reps())
                .weightKg(request.weightKg())
                .notes(request.notes())
                .position(position)
                .exerciseDefinitionId(request.exerciseDefinitionId())
                .rpe(request.rpe())
                .primaryMuscle(primaryMuscle)
                .build();

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public Exercise updateExercise(UUID sessionId, UUID exerciseId, UUID userId, UpdateExerciseRequest request) {
        // Verify session ownership
        findById(sessionId, userId);
        Exercise exercise = exerciseRepository.findByIdAndWorkoutSessionUserId(exerciseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));

        String primaryMuscle = request.exerciseDefinitionId() != null
                ? resolvePrimaryMuscle(request.exerciseDefinitionId())
                : null;
        exercise.applyPatch(request.name(), request.sets(), request.reps(),
                request.weightKg(), request.notes(), request.position(),
                request.exerciseDefinitionId(), request.rpe(), primaryMuscle);

        return exerciseRepository.save(exercise);
    }

    @Transactional
    public WorkoutSet updateSet(UUID sessionId, UUID exerciseId, int setNumber, UUID userId,
                                Integer reps, java.math.BigDecimal weightKg) {
        // Verify session ownership
        findById(sessionId, userId);
        WorkoutSet ws = workoutSetRepository
                .findByExerciseIdAndSetNumberAndUserId(exerciseId, setNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSet",
                        exerciseId + "/" + setNumber));
        ws.update(reps, weightKg);
        return workoutSetRepository.save(ws);
    }

    @Transactional
    public void deleteExercise(UUID sessionId, UUID exerciseId, UUID userId) {
        findById(sessionId, userId);
        Exercise exercise = exerciseRepository.findByIdAndWorkoutSessionUserId(exerciseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));
        exerciseRepository.delete(exercise);
    }

    private String resolvePrimaryMuscle(java.util.UUID definitionId) {
        if (definitionId == null) return null;
        return definitionRepository.findById(definitionId)
                .map(com.deska.evolvelog.domain.ExerciseDefinition::getPrimaryMuscle)
                .orElse(null);
    }

    private TrainingPlan resolveTrainingPlan(UUID planId, UUID userId) {
        if (planId == null) return null;
        return trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingPlan", planId));
    }

    private List<Exercise> buildExercises(List<CreateExerciseRequest> requests, WorkoutSession session) {
        List<Exercise> exercises = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            CreateExerciseRequest req = requests.get(i);
            String primaryMuscle = resolvePrimaryMuscle(req.exerciseDefinitionId());
            exercises.add(Exercise.builder()
                    .workoutSession(session)
                    .name(req.name())
                    .sets(req.sets())
                    .reps(req.reps())
                    .weightKg(req.weightKg())
                    .notes(req.notes())
                    .position(req.position() != null ? req.position() : i)
                    .exerciseDefinitionId(req.exerciseDefinitionId())
                    .rpe(req.rpe())
                    .primaryMuscle(primaryMuscle)
                    .build());
        }
        return exercises;
    }
}
