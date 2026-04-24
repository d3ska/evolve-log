package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.CreateExerciseRequest;
import com.deska.evolvelog.dto.request.CreateWorkoutSessionRequest;
import com.deska.evolvelog.dto.request.UpdateExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateWorkoutSessionRequest;
import com.deska.evolvelog.dto.response.ExerciseDto;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.service.WorkoutService;
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

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
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
                .map(WorkoutSessionDto::summary)
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
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        workoutService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
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
    public ResponseEntity<ApiResponse<Void>> deleteExercise(
            @PathVariable UUID id,
            @PathVariable UUID exerciseId,
            @AuthenticationPrincipal User user) {

        workoutService.deleteExercise(id, exerciseId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
