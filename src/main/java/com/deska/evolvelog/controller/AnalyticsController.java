package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.AnalyticsDto;
import com.deska.evolvelog.dto.response.BiweeklyReportDto;
import com.deska.evolvelog.dto.response.ExerciseProgressDto;
import com.deska.evolvelog.service.AnalyticsService;
import com.deska.evolvelog.service.BiweeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final BiweeklyReportService biweeklyReportService;

    /**
     * GET /api/analytics?from=2025-01-01&to=2025-12-31
     * Returns measurement trends, body fat trend, personal records, and workout count for the period.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsDto>> getAnalytics(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AnalyticsDto data = analyticsService.getAnalytics(user.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/analytics/report?from=2025-01-01&to=2025-01-14
     * Returns a full bi-weekly report: measurements delta, workout summary, sessions, and new PRs.
     */
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<BiweeklyReportDto>> getBiweeklyReport(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        BiweeklyReportDto report = biweeklyReportService.generateReport(user.getId(), from, to);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * GET /api/analytics/progress?exerciseName=Bench+Press
     * GET /api/analytics/progress?exerciseName=Bench+Press&planId=UUID
     * Returns the full history of a specific exercise for the user (optionally scoped to a training plan).
     */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<ExerciseProgressDto>> getExerciseProgress(
            @AuthenticationPrincipal User user,
            @RequestParam String exerciseName,
            @RequestParam(required = false) UUID planId
    ) {
        ExerciseProgressDto progress = analyticsService.getExerciseProgress(user.getId(), exerciseName, planId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * GET /api/analytics/exercises
     * Returns all distinct exercise names the user has ever logged (for autocomplete).
     */
    @GetMapping("/exercises")
    public ResponseEntity<ApiResponse<List<String>>> getExerciseNames(
            @AuthenticationPrincipal User user
    ) {
        List<String> names = analyticsService.getDistinctExerciseNames(user.getId());
        return ResponseEntity.ok(ApiResponse.success(names));
    }
}
