package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.repository.FitatuFoodLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class FitatuImportService {

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
    private final FitatuFoodLogRepository foodLogRepository;
    private final ObjectMapper objectMapper;

    public FitatuImportService(FitatuCsvParser csvParser,
                               HealthMetricService healthMetricService,
                               FitatuFoodLogRepository foodLogRepository,
                               ObjectMapper objectMapper) {
        this.csvParser = csvParser;
        this.healthMetricService = healthMetricService;
        this.foodLogRepository = foodLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a Fitatu CSV, upserts daily aggregates into health_metrics,
     * and upserts raw food log rows into fitatu_food_logs.
     *
     * @return the number of health_metric rows upserted
     */
    @Transactional
    public int importCsv(User user, InputStream inputStream) throws IOException {
        FitatuCsvParser.ParseResult parsed = csvParser.parse(inputStream);

        int count = 0;
        for (Map.Entry<LocalDate, Map<String, BigDecimal>> dayEntry : parsed.dailyTotals().entrySet()) {
            LocalDate date = dayEntry.getKey();
            for (Map.Entry<String, BigDecimal> metric : dayEntry.getValue().entrySet()) {
                String key  = metric.getKey();
                String unit = UNITS.getOrDefault(key, "");
                healthMetricService.upsert(user, "fitatu", date, key, metric.getValue(), unit);
                count++;
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (FitatuCsvParser.FoodLogRow row : parsed.rawRows()) {
            String meal     = row.meal()     != null ? row.meal()     : "";
            String foodName = row.foodName() != null ? row.foodName() : "";
            String nutrientsJson = toJson(row.nutrients());
            foodLogRepository.upsert(user.getId(), row.date(), meal, foodName, row.quantityG(), nutrientsJson, now);
        }

        return count;
    }

    private String toJson(Map<String, BigDecimal> nutrients) {
        try {
            return objectMapper.writeValueAsString(nutrients != null ? nutrients : Map.of());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
