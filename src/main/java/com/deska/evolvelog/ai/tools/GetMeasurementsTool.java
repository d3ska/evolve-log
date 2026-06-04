package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetMeasurementsTool implements AiTool {

    private final MeasurementRepository measurementRepository;

    public GetMeasurementsTool(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    @Override
    public String name() {
        return "get_measurements";
    }

    @Override
    public String description() {
        return "Returns body measurements for the athlete including weight, body fat %, and circumference measurements. " +
               "Supports date range queries (from_date/to_date) for trend analysis, or limit-based for most recent entries.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of(
                                "type", "integer",
                                "description", "Number of most-recent entries to return (default 10, max 50). Used when no date range is given."
                        ),
                        "from_date", Map.of(
                                "type", "string",
                                "description", "Start date YYYY-MM-DD. When provided, returns all entries in range ordered oldest first (ignores limit)."
                        ),
                        "to_date", Map.of(
                                "type", "string",
                                "description", "End date YYYY-MM-DD (default: today). Used together with from_date."
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        String fromStr = input.path("from_date").asText(null);
        String toStr = input.path("to_date").asText(null);

        if (fromStr != null && !fromStr.isBlank()) {
            return executeDateRange(userId, fromStr, toStr);
        }
        return executeLimit(userId, input.path("limit").asInt(10));
    }

    private String executeDateRange(UUID userId, String fromStr, String toStr) {
        LocalDate to = parseDate(toStr, LocalDate.now());
        LocalDate from = parseDate(fromStr, to.minusDays(90));

        List<Measurement> measurements = measurementRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        if (measurements.isEmpty()) {
            return "No measurements found for " + from + " to " + to + ".";
        }
        StringBuilder sb = new StringBuilder("Body measurements (")
                .append(from).append(" to ").append(to)
                .append(", ").append(measurements.size()).append(" entries):\n");
        measurements.forEach(m -> appendMeasurement(sb, m));
        return sb.toString();
    }

    private String executeLimit(UUID userId, int requestedLimit) {
        int limit = Math.min(requestedLimit, 50);
        List<Measurement> measurements = measurementRepository
                .findByUserIdOrderByDateDesc(userId, PageRequest.of(0, limit))
                .getContent();

        if (measurements.isEmpty()) {
            return "No measurements found.";
        }
        StringBuilder sb = new StringBuilder("Body measurements (most recent ")
                .append(measurements.size()).append(" entries):\n");
        measurements.forEach(m -> appendMeasurement(sb, m));
        return sb.toString();
    }

    private void appendMeasurement(StringBuilder sb, Measurement m) {
        sb.append("- ").append(m.getDate());
        if (m.getWeightKg() != null)       sb.append(", weight: ").append(m.getWeightKg()).append(" kg");
        if (m.getBodyFatPercent() != null)  sb.append(", body fat: ").append(m.getBodyFatPercent()).append("%");
        if (m.getWaistNavelCm() != null)    sb.append(", waist: ").append(m.getWaistNavelCm()).append(" cm");
        if (m.getBicepsCm() != null)        sb.append(", biceps: ").append(m.getBicepsCm()).append(" cm");
        if (m.getChestCm() != null)         sb.append(", chest: ").append(m.getChestCm()).append(" cm");
        if (m.getThighCm() != null)         sb.append(", thigh: ").append(m.getThighCm()).append(" cm");
        sb.append("\n");
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
