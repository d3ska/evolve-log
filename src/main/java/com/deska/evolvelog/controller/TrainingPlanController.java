package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.CreatePlannedExerciseRequest;
import com.deska.evolvelog.dto.request.CreateTrainingPlanRequest;
import com.deska.evolvelog.dto.request.UpdatePlannedExerciseRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingPlanRequest;
import com.deska.evolvelog.dto.response.PlannedExerciseDto;
import com.deska.evolvelog.dto.response.TrainingPlanDto;
import com.deska.evolvelog.dto.response.TrainingPlanVersionDto;
import com.deska.evolvelog.service.TrainingPlanService;
import com.deska.evolvelog.service.TrainingPlanVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService planService;
    private final TrainingPlanVersionService versionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TrainingPlanDto>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTrainingPlanRequest request
    ) {
        TrainingPlanDto dto = TrainingPlanDto.from(planService.create(user, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainingPlanDto>>> getAll(
            @AuthenticationPrincipal User user
    ) {
        List<TrainingPlanDto> plans = planService.findAll(user.getId()).stream()
                .map(TrainingPlanDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        TrainingPlanDto dto = TrainingPlanDto.from(planService.findById(id, user.getId()));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrainingPlanRequest request
    ) {
        TrainingPlanDto dto = TrainingPlanDto.from(planService.update(id, user.getId(), request));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        planService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    // --- Planned Exercises ---

    @PostMapping("/{planId}/exercises")
    public ResponseEntity<ApiResponse<PlannedExerciseDto>> addExercise(
            @AuthenticationPrincipal User user,
            @PathVariable UUID planId,
            @Valid @RequestBody CreatePlannedExerciseRequest request
    ) {
        PlannedExerciseDto dto = PlannedExerciseDto.from(planService.addExercise(planId, user.getId(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PatchMapping("/{planId}/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<PlannedExerciseDto>> updateExercise(
            @AuthenticationPrincipal User user,
            @PathVariable UUID planId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody UpdatePlannedExerciseRequest request
    ) {
        PlannedExerciseDto dto = PlannedExerciseDto.from(
                planService.updateExercise(planId, exerciseId, user.getId(), request));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{planId}/exercises/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(
            @AuthenticationPrincipal User user,
            @PathVariable UUID planId,
            @PathVariable UUID exerciseId
    ) {
        planService.deleteExercise(planId, exerciseId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<TrainingPlanVersionDto>>> getVersions(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        List<TrainingPlanVersionDto> versions = versionService.listVersions(id, user.getId()).stream()
                .map(TrainingPlanVersionDto::summary)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<ApiResponse<TrainingPlanVersionDto>> getVersion(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @PathVariable Integer version
    ) {
        TrainingPlanVersionDto dto = TrainingPlanVersionDto.full(versionService.getVersion(id, version, user.getId()));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/{planId}/sync-from-session/{sessionId}")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> syncFromSession(
            @AuthenticationPrincipal User user,
            @PathVariable UUID planId,
            @PathVariable UUID sessionId
    ) {
        TrainingPlanDto dto = TrainingPlanDto.from(planService.syncFromSession(planId, sessionId, user.getId()));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
