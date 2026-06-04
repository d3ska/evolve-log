package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.service.HealthMetricService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class GetHealthMetricsTool implements AiTool {

    private static final Map<String, String> LABEL_MAP = Map.ofEntries(
            Map.entry("basal_metabolic_rate", "bmr"),
            Map.entry("body_fat_percent", "body_fat"),
            Map.entry("heart_pulse_bpm", "heart_pulse"),
            Map.entry("fat_free_mass_kg", "lean_mass_kg"),
            Map.entry("fat_mass_weight_kg", "fat_mass_kg"),
            Map.entry("pulse_wave_velocity", "pulse_wave_velocity"),
            Map.entry("nerve_health_score", "nerve_health")
    );

    private final HealthMetricService healthMetricService;

    public GetHealthMetricsTool(HealthMetricService healthMetricService) {
        this.healthMetricService = healthMetricService;
    }

    @Override
    public String name() {
        return "get_health_metrics";
    }

    @Override
    public String description() {
        return "Returns device-measured health metrics from Withings: body composition " +
               "(weight, body fat %, muscle mass, bone mass), basal metabolic rate (BMR), " +
               "VO2max, visceral fat, resting heart rate, and more. Use for body composition " +
               "trends, calorie target estimation via BMR, cardiovascular fitness, or recovery monitoring.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "from_date", Map.of(
                                "type", "string",
                                "description", "Start date YYYY-MM-DD (default: 30 days ago)"
                        ),
                        "to_date", Map.of(
                                "type", "string",
                                "description", "End date YYYY-MM-DD (default: today)"
                        ),
                        "metric_keys", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Optional filter to specific metric keys e.g. [\"weight_kg\",\"vo2_max\"]. Omit for all."
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        LocalDate to = parseDate(input.path("to_date").asText(null), LocalDate.now());
        LocalDate from = parseDate(input.path("from_date").asText(null), to.minusDays(30));

        final Set<String> keyFilter;
        JsonNode keysNode = input.path("metric_keys");
        if (keysNode.isArray() && !keysNode.isEmpty()) {
            keyFilter = StreamSupport.stream(keysNode.spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toSet());
        } else {
            keyFilter = null;
        }

        List<DailyHealthMetricsDto> days = healthMetricService.getDailyMetrics(userId, "withings", from, to);

        if (days.isEmpty()) {
            return "No Withings health metrics found for " + from + " to " + to + ". " +
                   "Sync your Withings device to populate this data.";
        }

        StringBuilder sb = new StringBuilder("Health metrics (Withings) — ")
                .append(from).append(" to ").append(to).append(":\n\n");

        for (DailyHealthMetricsDto day : days) {
            Map<String, BigDecimal> metrics = day.metrics();
            if (keyFilter != null) {
                metrics = metrics.entrySet().stream()
                        .filter(e -> keyFilter.contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (metrics.isEmpty()) continue;

            sb.append("- **").append(day.date()).append("**: ");
            boolean first = true;
            for (Map.Entry<String, BigDecimal> entry : metrics.entrySet()) {
                if (!first) sb.append(", ");
                String label = LABEL_MAP.getOrDefault(entry.getKey(), entry.getKey());
                sb.append(label).append(" ").append(entry.getValue().stripTrailingZeros().toPlainString());
                first = false;
            }
            sb.append("\n");
        }

        return sb.toString().stripTrailing();
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
