package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.service.FitatuImportService;
import com.deska.evolvelog.service.HealthMetricService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
public class NutritionController {

    private static final String FITATU_SOURCE = "fitatu";

    private final FitatuImportService fitatuImportService;
    private final HealthMetricService healthMetricService;

    public NutritionController(FitatuImportService fitatuImportService,
                               HealthMetricService healthMetricService) {
        this.fitatuImportService = fitatuImportService;
        this.healthMetricService = healthMetricService;
    }

    @PostMapping("/api/nutrition/upload")
    public ResponseEntity<ApiResponse<Integer>> upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        try {
            int count = fitatuImportService.importCsv(user, file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (IOException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to parse CSV: " + e.getMessage());
        }
    }

    @GetMapping("/api/nutrition/daily")
    public ResponseEntity<ApiResponse<List<DailyHealthMetricsDto>>> daily(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DailyHealthMetricsDto> data = healthMetricService.getDailyMetrics(user.getId(), FITATU_SOURCE, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
