package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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
               "Most recent entries first.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of(
                                "type", "integer",
                                "description", "Number of measurement entries to return (default 10, max 50)"
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        int limit = Math.min(input.path("limit").asInt(10), 50);
        List<Measurement> measurements = measurementRepository
                .findByUserIdOrderByDateDesc(userId, PageRequest.of(0, limit))
                .getContent();

        if (measurements.isEmpty()) {
            return "No measurements found.";
        }
        StringBuilder sb = new StringBuilder("Body measurements:\n");
        measurements.forEach(m -> {
            sb.append("- ").append(m.getDate());
            if (m.getWeightKg() != null)       sb.append(", weight: ").append(m.getWeightKg()).append(" kg");
            if (m.getBodyFatPercent() != null)  sb.append(", body fat: ").append(m.getBodyFatPercent()).append("%");
            if (m.getWaistNavelCm() != null)    sb.append(", waist: ").append(m.getWaistNavelCm()).append(" cm");
            if (m.getBicepsCm() != null)        sb.append(", biceps: ").append(m.getBicepsCm()).append(" cm");
            if (m.getChestCm() != null)         sb.append(", chest: ").append(m.getChestCm()).append(" cm");
            if (m.getThighCm() != null)         sb.append(", thigh: ").append(m.getThighCm()).append(" cm");
            sb.append("\n");
        });
        return sb.toString();
    }
}
