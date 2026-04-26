package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.CreateExerciseDefinitionRequest;
import com.deska.evolvelog.dto.response.ExerciseDefinitionDto;
import com.deska.evolvelog.service.ExerciseAutoLinkService;
import com.deska.evolvelog.service.ExerciseDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseDefinitionController {

    private final ExerciseDefinitionService definitionService;
    private final ExerciseAutoLinkService autoLinkService;

    public ExerciseDefinitionController(ExerciseDefinitionService definitionService,
                                        ExerciseAutoLinkService autoLinkService) {
        this.definitionService = definitionService;
        this.autoLinkService = autoLinkService;
    }

    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<List<ExerciseDefinitionDto>>> listDefinitions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String muscle) {
        List<ExerciseDefinitionDto> result = definitionService.listDefinitions(user.getId(), q, muscle);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/definitions")
    public ResponseEntity<ApiResponse<ExerciseDefinitionDto>> createDefinition(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateExerciseDefinitionRequest request) {
        ExerciseDefinitionDto created = definitionService.createUserDefinition(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping("/definitions/muscle-groups")
    public ResponseEntity<ApiResponse<List<String>>> getMuscleGroups() {
        return ResponseEntity.ok(ApiResponse.success(definitionService.getMuscleGroups()));
    }

    @PostMapping("/auto-link")
    public ResponseEntity<ApiResponse<com.deska.evolvelog.dto.response.AutoLinkResultDto>> autoLink(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(autoLinkService.autoLink(user.getId())));
    }
}
