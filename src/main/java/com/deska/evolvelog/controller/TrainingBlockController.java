package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.CreateTrainingBlockRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingBlockRequest;
import com.deska.evolvelog.dto.response.TrainingBlockDto;
import com.deska.evolvelog.service.TrainingBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/training-blocks")
@RequiredArgsConstructor
public class TrainingBlockController {

    private final TrainingBlockService blockService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainingBlockDto>>> list(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success(blockService.list(user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrainingBlockDto>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTrainingBlockRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(blockService.create(user, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainingBlockDto>> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody UpdateTrainingBlockRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(blockService.update(user, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean deletePlans
    ) {
        blockService.delete(user, id, deletePlans);
        return ResponseEntity.noContent().build();
    }
}
