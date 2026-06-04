package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.AiSettings;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.SaveAiSettingsRequest;
import com.deska.evolvelog.dto.request.UpdateGoalsRequest;
import com.deska.evolvelog.dto.response.AiSettingsView;
import com.deska.evolvelog.service.AiSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/ai/settings")
public class AiSettingsController {

    private final AiSettingsService aiSettingsService;

    public AiSettingsController(AiSettingsService aiSettingsService) {
        this.aiSettingsService = aiSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AiSettingsView>> getSettings(
            @AuthenticationPrincipal User user) {

        AiSettingsView view = aiSettingsService.getSettings(user.getId())
                .map(AiSettingsView::from)
                .orElseGet(AiSettingsView::empty);
        return ResponseEntity.ok(ApiResponse.success(view));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AiSettingsView>> saveSettings(
            @Valid @RequestBody SaveAiSettingsRequest request,
            @AuthenticationPrincipal User user) {

        AiSettings saved = aiSettingsService.saveSettings(user.getId(), request.provider(), request.apiKey());
        return ResponseEntity.ok(ApiResponse.success(AiSettingsView.from(saved)));
    }

    @PutMapping("/goals")
    public ResponseEntity<Void> updateGoals(
            @Valid @RequestBody UpdateGoalsRequest request,
            @AuthenticationPrincipal User user) {

        aiSettingsService.updateGoals(user.getId(), request.goals());
        return ResponseEntity.status(NO_CONTENT).build();
    }
}
