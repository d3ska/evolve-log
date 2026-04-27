package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.FitatuFoodLog;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.repository.FitatuFoodLogRepository;
import com.deska.evolvelog.service.HealthMetricService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetNutritionLogTool implements AiTool {

    private static final Logger log = LoggerFactory.getLogger(GetNutritionLogTool.class);
    private static final String FITATU_SOURCE = "fitatu";

    private final FitatuFoodLogRepository fitatuFoodLogRepository;
    private final HealthMetricService healthMetricService;

    public GetNutritionLogTool(FitatuFoodLogRepository fitatuFoodLogRepository,
                               HealthMetricService healthMetricService) {
        this.fitatuFoodLogRepository = fitatuFoodLogRepository;
        this.healthMetricService = healthMetricService;
    }

    @Override
    public String name() {
        return "get_nutrition_log";
    }

    @Override
    public String description() {
        return "Returns the athlete's food diary entries imported from Fitatu for a given date range. " +
                "Includes meal name, food name, quantity, and macronutrients (kcal, protein, carbs, fat). " +
                "Use this when asked about diet, calories, macros, or what the athlete ate.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "from_date", Map.of(
                                "type", "string",
                                "description", "Start date in YYYY-MM-DD format (default: 30 days ago)"
                        ),
                        "to_date", Map.of(
                                "type", "string",
                                "description", "End date in YYYY-MM-DD format (default: today)"
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        LocalDate to = parseDate(input.path("to_date").asText(null), LocalDate.now());
        LocalDate from = parseDate(input.path("from_date").asText(null), to.minusDays(30));

        log.info("get_nutrition_log: querying userId={} from={} to={}", userId, from, to);
        List<FitatuFoodLog> logs = fitatuFoodLogRepository.findByUserIdAndDateBetween(userId, from, to);
        log.info("get_nutrition_log: found {} raw rows", logs.size());

        if (!logs.isEmpty()) {
            return formatRawRows(logs, from, to);
        }

        // Fallback to daily aggregates from health_metrics (same source the nutrition page uses)
        log.info("get_nutrition_log: raw rows empty, falling back to health_metrics");
        List<DailyHealthMetricsDto> dailyMetrics = healthMetricService.getDailyMetrics(userId, FITATU_SOURCE, from, to);
        log.info("get_nutrition_log: found {} daily aggregate rows", dailyMetrics.size());

        if (dailyMetrics.isEmpty()) {
            return "No nutrition data found for " + from + " to " + to + ". The user may not have imported any Fitatu data yet.";
        }

        return formatDailyAggregates(dailyMetrics, from, to);
    }

    private String formatRawRows(List<FitatuFoodLog> logs, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("Nutrition log (" + from + " to " + to + "):\n\n");
        LocalDate currentDate = null;
        BigDecimal dayKcal = BigDecimal.ZERO, dayProtein = BigDecimal.ZERO,
                dayCarbs = BigDecimal.ZERO, dayFat = BigDecimal.ZERO;

        for (FitatuFoodLog entry : logs) {
            if (!entry.getDate().equals(currentDate)) {
                if (currentDate != null) {
                    appendDayTotals(sb, dayKcal, dayProtein, dayCarbs, dayFat);
                    dayKcal = dayProtein = dayCarbs = dayFat = BigDecimal.ZERO;
                }
                currentDate = entry.getDate();
                sb.append("### ").append(currentDate).append("\n");
            }
            Map<String, BigDecimal> n = entry.getNutrients();
            BigDecimal kcal = n.getOrDefault("kcal", n.getOrDefault("calories", BigDecimal.ZERO));
            BigDecimal protein = n.getOrDefault("protein_g", n.getOrDefault("protein", BigDecimal.ZERO));
            BigDecimal carbs = n.getOrDefault("carbs_g", n.getOrDefault("carbs", BigDecimal.ZERO));
            BigDecimal fat = n.getOrDefault("fat_g", n.getOrDefault("fat", BigDecimal.ZERO));

            sb.append(String.format("- [%s] %s", entry.getMeal() != null ? entry.getMeal() : "?", entry.getFoodName()));
            if (entry.getQuantityG() != null) sb.append(String.format(" %.0fg", entry.getQuantityG()));
            sb.append(String.format(" — %.0f kcal, P:%.1fg, C:%.1fg, F:%.1fg%n", kcal, protein, carbs, fat));

            dayKcal = dayKcal.add(kcal);
            dayProtein = dayProtein.add(protein);
            dayCarbs = dayCarbs.add(carbs);
            dayFat = dayFat.add(fat);
        }
        if (currentDate != null) appendDayTotals(sb, dayKcal, dayProtein, dayCarbs, dayFat);
        return sb.toString();
    }

    private String formatDailyAggregates(List<DailyHealthMetricsDto> days, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("Nutrition log — daily totals (" + from + " to " + to + "):\n\n");
        for (DailyHealthMetricsDto day : days) {
            Map<String, BigDecimal> m = day.metrics();
            BigDecimal kcal = m.getOrDefault("kcal", BigDecimal.ZERO);
            BigDecimal protein = m.getOrDefault("protein_g", BigDecimal.ZERO);
            BigDecimal carbs = m.getOrDefault("carbs_g", BigDecimal.ZERO);
            BigDecimal fat = m.getOrDefault("fat_g", BigDecimal.ZERO);
            BigDecimal fiber = m.getOrDefault("fiber_g", BigDecimal.ZERO);
            sb.append(String.format("- **%s**: %.0f kcal, P:%.1fg, C:%.1fg, F:%.1fg, fiber:%.1fg%n",
                    day.date(), kcal, protein, carbs, fat, fiber));
        }
        return sb.toString();
    }

    private void appendDayTotals(StringBuilder sb, BigDecimal kcal, BigDecimal protein,
                                  BigDecimal carbs, BigDecimal fat) {
        sb.append(String.format("Daily totals: %.0f kcal, protein %.1fg, carbs %.1fg, fat %.1fg%n%n",
                kcal, protein, carbs, fat));
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
