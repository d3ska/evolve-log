package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.MonthlyExerciseAggregate;
import com.deska.evolvelog.repository.MonthlyAggregateRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetMonthlyAggregatesTool implements AiTool {

    private final MonthlyAggregateRepository monthlyAggregateRepository;

    public GetMonthlyAggregatesTool(MonthlyAggregateRepository monthlyAggregateRepository) {
        this.monthlyAggregateRepository = monthlyAggregateRepository;
    }

    @Override
    public String name() {
        return "get_monthly_aggregates";
    }

    @Override
    public String description() {
        return "Returns pre-computed monthly aggregates for a specific exercise: " +
               "max weight, total volume, session count, and total sets per month.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "exercise_name", Map.of(
                                "type", "string",
                                "description", "The exact exercise name to look up"
                        )
                ),
                "required", List.of("exercise_name")
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        String exerciseName = input.path("exercise_name").asText();
        if (exerciseName.isBlank()) {
            return "Error: exercise_name is required.";
        }
        List<MonthlyExerciseAggregate> aggregates =
                monthlyAggregateRepository.findByUserIdAndExerciseNameOrderByYearMonthDesc(userId, exerciseName);
        if (aggregates.isEmpty()) {
            return "No monthly aggregates found for: " + exerciseName;
        }
        StringBuilder sb = new StringBuilder("Monthly aggregates for ").append(exerciseName).append(":\n");
        aggregates.forEach(a -> {
            LocalDate ym = a.getYearMonth();
            sb.append("- ").append(ym.getYear()).append("-").append(String.format("%02d", ym.getMonthValue()))
                    .append(": max ").append(a.getMaxWeightKg()).append(" kg")
                    .append(", volume ").append(a.getTotalVolumeKg()).append(" kg")
                    .append(", ").append(a.getSessionCount()).append(" sessions")
                    .append(", ").append(a.getTotalSets()).append(" sets\n");
        });
        return sb.toString();
    }
}
