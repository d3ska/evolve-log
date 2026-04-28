package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.ProgressiveOverloadDto;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.dto.response.WeeklyMuscleVolumeDto;
import com.deska.evolvelog.service.ProgressiveOverloadService;
import com.deska.evolvelog.service.TrainingVolumeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class TrainingAnalyticsController {

    private final TrainingVolumeService volumeService;
    private final ProgressiveOverloadService overloadService;

    public TrainingAnalyticsController(TrainingVolumeService volumeService,
                                       ProgressiveOverloadService overloadService) {
        this.volumeService = volumeService;
        this.overloadService = overloadService;
    }

    @GetMapping("/sessions/{sessionId}/volume")
    public ResponseEntity<ApiResponse<SessionVolumeSummaryDto>> getSessionVolume(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        SessionVolumeSummaryDto summary = volumeService.getSessionVolumeSummary(sessionId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/volume/weekly")
    public ResponseEntity<ApiResponse<List<WeeklyMuscleVolumeDto>>> getWeeklyVolume(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String muscle) {
        List<WeeklyMuscleVolumeDto> result = volumeService.getWeeklyVolumeByMuscle(user.getId(), from, to, muscle);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/progressive-overload/{exerciseDefinitionId}")
    public ResponseEntity<ApiResponse<ProgressiveOverloadDto>> getProgressiveOverload(
            @AuthenticationPrincipal User user,
            @PathVariable UUID exerciseDefinitionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "12") int sessions) {
        ProgressiveOverloadDto result = (from != null && to != null)
                ? overloadService.getProgressiveOverload(user.getId(), exerciseDefinitionId, from, to)
                : overloadService.getProgressiveOverload(user.getId(), exerciseDefinitionId, Math.min(sessions, 200));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
