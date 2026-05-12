package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.AddExerciseToSessionRequest;
import com.deska.evolvelog.dto.request.AddWorkoutSetRequest;
import com.deska.evolvelog.dto.request.CreateExerciseRequest;
import com.deska.evolvelog.dto.request.CreateWorkoutSessionRequest;
import com.deska.evolvelog.dto.request.PatchWorkoutSetRequest;
import com.deska.evolvelog.dto.request.StartWorkoutSessionRequest;
import com.deska.evolvelog.dto.request.UpdateExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateWorkoutSessionRequest;
import com.deska.evolvelog.dto.response.ExerciseDto;
import com.deska.evolvelog.dto.response.FinishedSessionDto;
import com.deska.evolvelog.dto.response.SessionDeviationDto;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.dto.response.WorkoutSetDto;
import com.deska.evolvelog.service.WorkoutDeviationService;
import com.deska.evolvelog.service.WorkoutService;
import com.deska.evolvelog.service.WorkoutSessionFlowService;
import com.deska.evolvelog.service.WorkoutSetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final WorkoutSessionFlowService flowService;
    private final WorkoutSetService workoutSetService;
    private final WorkoutDeviationService deviationService;

    public WorkoutController(WorkoutService workoutService, WorkoutSessionFlowService flowService,
                             WorkoutSetService workoutSetService, WorkoutDeviationService deviationService) {
        this.workoutService = workoutService;
        this.flowService = flowService;
        this.workoutSetService = workoutSetService;
        this.deviationService = deviationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutSessionDto>> create(
            @Valid @RequestBody CreateWorkoutSessionRequest request,
            @AuthenticationPrincipal User user) {

        WorkoutSession session = workoutService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(WorkoutSessionDto.from(session)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutSessionDto>>> findAll(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<WorkoutSessionDto> sessions = workoutService.findAll(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.paged(sessions.getContent(), sessions.getTotalElements(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkoutSessionDto>> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        WorkoutSession session = workoutService.findById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(WorkoutSessionDto.from(session)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkoutSessionDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkoutSessionRequest request,
            @AuthenticationPrincipal User user) {

        WorkoutSession session = workoutService.update(id, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(WorkoutSessionDto.from(session)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        workoutService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/exercises")
    public ResponseEntity<ApiResponse<ExerciseDto>> addExercise(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal User user) {

        Exercise exercise = workoutService.addExercise(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ExerciseDto.from(exercise)));
    }

    @PatchMapping("/{id}/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<ExerciseDto>> updateExercise(
            @PathVariable UUID id,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody UpdateExerciseRequest request,
            @AuthenticationPrincipal User user) {

        Exercise exercise = workoutService.updateExercise(id, exerciseId, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(ExerciseDto.from(exercise)));
    }

    @DeleteMapping("/{id}/exercises/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(
            @PathVariable UUID id,
            @PathVariable UUID exerciseId,
            @AuthenticationPrincipal User user) {

        workoutService.deleteExercise(id, exerciseId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<WorkoutSessionDto>> getActiveSession(@AuthenticationPrincipal User user) {
        return flowService.getActiveSession(user.getId())
                .map(s -> ResponseEntity.ok(ApiResponse.success(WorkoutSessionDto.from(s))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    @PostMapping("/exercises")
    public ResponseEntity<ApiResponse<ExerciseDto>> addExerciseToSession(
            @Valid @RequestBody AddExerciseToSessionRequest request,
            @AuthenticationPrincipal User user) {

        Exercise exercise = flowService.addExercise(request.sessionId(), user.getId(), request.name(), request.sets());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ExerciseDto.from(exercise)));
    }

    @DeleteMapping("/exercises/{exerciseId}")
    public ResponseEntity<Void> removeExercise(
            @PathVariable UUID exerciseId,
            @AuthenticationPrincipal User user) {

        flowService.removeExercise(exerciseId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sets")
    public ResponseEntity<ApiResponse<WorkoutSetDto>> addSet(
            @Valid @RequestBody AddWorkoutSetRequest request,
            @AuthenticationPrincipal User user) {

        WorkoutSet ws = workoutSetService.addSet(request.exerciseId(), user.getId(),
                request.setNumber(), request.reps(), request.weightKg());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(WorkoutSetDto.from(ws)));
    }

    @PatchMapping("/sets/{setId}")
    public ResponseEntity<ApiResponse<WorkoutSetDto>> updateSet(
            @PathVariable UUID setId,
            @RequestBody PatchWorkoutSetRequest request,
            @AuthenticationPrincipal User user) {

        WorkoutSet ws = workoutSetService.updateSet(setId, user.getId(),
                request.reps(), request.weightKg(), request.completed());
        return ResponseEntity.ok(ApiResponse.success(WorkoutSetDto.from(ws)));
    }

    @DeleteMapping("/sets/{setId}")
    public ResponseEntity<Void> deleteSet(
            @PathVariable UUID setId,
            @AuthenticationPrincipal User user) {

        workoutSetService.deleteSet(setId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/start")
    public ResponseEntity<ApiResponse<WorkoutSessionDto>> startSession(
            @Valid @RequestBody StartWorkoutSessionRequest request,
            @AuthenticationPrincipal User user) {

        WorkoutSession session = flowService.startFromPlan(user, request.trainingPlanId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(WorkoutSessionDto.from(session)));
    }

    @GetMapping("/sessions/{id}/deviations")
    public ResponseEntity<ApiResponse<SessionDeviationDto>> getDeviations(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        SessionDeviationDto result = deviationService.getDeviations(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/sessions/{id}/finish")
    public ResponseEntity<ApiResponse<FinishedSessionDto>> finishSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        FinishedSessionDto result = flowService.finishSession(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
