package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.CreateExerciseRequest;
import com.deska.evolvelog.dto.request.CreateWorkoutSessionRequest;
import com.deska.evolvelog.dto.request.StartWorkoutSessionRequest;
import com.deska.evolvelog.dto.request.UpdateExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateWorkoutSessionRequest;
import com.deska.evolvelog.dto.response.ExerciseDto;
import com.deska.evolvelog.dto.response.FinishedSessionDto;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.service.WorkoutService;
import com.deska.evolvelog.service.WorkoutSessionFlowService;
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

    public WorkoutController(WorkoutService workoutService, WorkoutSessionFlowService flowService) {
        this.workoutService = workoutService;
        this.flowService = flowService;
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

        Page<WorkoutSession> sessions = workoutService.findAll(user.getId(), page, size);
        List<WorkoutSessionDto> dtos = sessions.getContent().stream()
                .map(WorkoutSessionDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.paged(dtos, sessions.getTotalElements(), page, size));
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

    @PostMapping("/sessions/start")
    public ResponseEntity<ApiResponse<WorkoutSessionDto>> startSession(
            @Valid @RequestBody StartWorkoutSessionRequest request,
            @AuthenticationPrincipal User user) {

        WorkoutSession session = flowService.startFromPlan(user, request.trainingPlanId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(WorkoutSessionDto.from(session)));
    }

    @PostMapping("/sessions/{id}/finish")
    public ResponseEntity<ApiResponse<FinishedSessionDto>> finishSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        FinishedSessionDto result = flowService.finishSession(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
