package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.WeeklyVolumeRow;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetVolumeStatsTool implements AiTool {

    private final ExerciseRepository exerciseRepository;

    public GetVolumeStatsTool(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    public String name() {
        return "get_volume_stats";
    }

    @Override
    public String description() {
        return "Returns weekly training volume by muscle group for a given number of weeks back. " +
               "Useful for assessing muscle balance and overload progression.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "weeks_back", Map.of(
                                "type", "integer",
                                "description", "Number of weeks to look back (default 8, max 52)"
                        ),
                        "muscle", Map.of(
                                "type", "string",
                                "description", "Optional muscle group filter (e.g. chest, back, legs). Omit for all muscles."
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        int weeksBack = Math.min(input.path("weeks_back").asInt(8), 52);
        String muscle = input.has("muscle") && !input.path("muscle").asText().isBlank()
                ? input.path("muscle").asText() : null;

        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusWeeks(weeksBack);

        List<WeeklyVolumeRow> rows =
                exerciseRepository.findWeeklyVolumeByMuscle(userId, from, to, muscle);

        if (rows.isEmpty()) {
            return "No volume data found for the requested period.";
        }
        StringBuilder sb = new StringBuilder("Weekly Volume by Muscle:\n");
        rows.forEach(r -> sb.append("- Week of ").append(r.getWeekStart())
                .append(" | ").append(r.getMuscle())
                .append(" | volume: ").append(r.getVolumeLoad()).append(" kg")
                .append(" | sessions: ").append(r.getSessionCount())
                .append(" | sets: ").append(r.getSetCount()).append("\n"));
        return sb.toString();
    }
}
