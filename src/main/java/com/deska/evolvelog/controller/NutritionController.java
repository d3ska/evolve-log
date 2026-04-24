package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.service.FitatuCsvParser;
import com.deska.evolvelog.service.HealthMetricService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class NutritionController {

    private static final String SOURCE = "fitatu";

    // Maps metric key suffix → unit string
    private static final Map<String, String> UNITS = Map.ofEntries(
            Map.entry("kcal",              "kcal"),
            Map.entry("protein_g",         "g"),
            Map.entry("protein_plant_g",   "g"),
            Map.entry("protein_animal_g",  "g"),
            Map.entry("fat_g",             "g"),
            Map.entry("fat_saturated_g",   "g"),
            Map.entry("fat_mono_g",        "g"),
            Map.entry("fat_poly_g",        "g"),
            Map.entry("omega3_g",          "g"),
            Map.entry("omega6_g",          "g"),
            Map.entry("carbs_g",           "g"),
            Map.entry("sugar_g",           "g"),
            Map.entry("cholesterol_mg",    "mg"),
            Map.entry("fiber_g",           "g"),
            Map.entry("caffeine_mg",       "mg"),
            Map.entry("folate_ug",         "ug"),
            Map.entry("vitamin_a_ug",      "ug"),
            Map.entry("vitamin_b1_mg",     "mg"),
            Map.entry("vitamin_b2_mg",     "mg"),
            Map.entry("vitamin_b5_mg",     "mg"),
            Map.entry("vitamin_b6_mg",     "mg"),
            Map.entry("biotin_ug",         "ug"),
            Map.entry("vitamin_b12_ug",    "ug"),
            Map.entry("vitamin_c_mg",      "mg"),
            Map.entry("vitamin_d_ug",      "ug"),
            Map.entry("vitamin_e_mg",      "mg"),
            Map.entry("niacin_mg",         "mg"),
            Map.entry("vitamin_k_ug",      "ug"),
            Map.entry("zinc_mg",           "mg"),
            Map.entry("phosphorus_mg",     "mg"),
            Map.entry("iodine_ug",         "ug"),
            Map.entry("magnesium_mg",      "mg"),
            Map.entry("copper_mg",         "mg"),
            Map.entry("potassium_mg",      "mg"),
            Map.entry("selenium_ug",       "ug"),
            Map.entry("sodium_mg",         "mg"),
            Map.entry("calcium_mg",        "mg"),
            Map.entry("iron_mg",           "mg"),
            Map.entry("salt_g",            "g")
    );

    private final FitatuCsvParser csvParser;
    private final HealthMetricService healthMetricService;

    public NutritionController(FitatuCsvParser csvParser, HealthMetricService healthMetricService) {
        this.csvParser = csvParser;
        this.healthMetricService = healthMetricService;
    }

    @PostMapping("/api/nutrition/upload")
    public ResponseEntity<ApiResponse<Integer>> upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        Map<LocalDate, Map<String, BigDecimal>> parsed;
        try {
            parsed = csvParser.parse(file.getInputStream());
        } catch (IOException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to parse CSV: " + e.getMessage());
        }

        int count = 0;
        for (Map.Entry<LocalDate, Map<String, BigDecimal>> dayEntry : parsed.entrySet()) {
            LocalDate date = dayEntry.getKey();
            for (Map.Entry<String, BigDecimal> metric : dayEntry.getValue().entrySet()) {
                String key = metric.getKey();
                String unit = UNITS.getOrDefault(key, "");
                healthMetricService.upsert(user, SOURCE, date, key, metric.getValue(), unit);
                count++;
            }
        }

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/api/nutrition/daily")
    public ResponseEntity<ApiResponse<List<DailyHealthMetricsDto>>> daily(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DailyHealthMetricsDto> data = healthMetricService.getDailyMetrics(user.getId(), SOURCE, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
