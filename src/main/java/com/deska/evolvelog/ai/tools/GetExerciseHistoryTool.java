package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.dto.response.ExerciseProgressPointDto;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetExerciseHistoryTool implements AiTool {

    private final ExerciseRepository exerciseRepository;

    public GetExerciseHistoryTool(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    public String name() {
        return "get_exercise_history";
    }

    @Override
    public String description() {
        return "Returns the full history of a specific exercise for the athlete, ordered by date. " +
               "Includes date, weight, sets, and reps for every logged session.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "exercise_name", Map.of(
                                "type", "string",
                                "description", "The exact name of the exercise (case-insensitive)"
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
        List<ExerciseProgressPointDto> history = exerciseRepository
                .findProgressByUserIdAndExerciseName(userId, exerciseName);
        if (history.isEmpty()) {
            return "No history found for exercise: " + exerciseName;
        }
        StringBuilder sb = new StringBuilder("History for ").append(exerciseName).append(":\n");
        history.forEach(p -> sb.append("- ").append(p.date())
                .append(": ").append(p.sets()).append("×").append(p.reps())
                .append(" @ ").append(p.weightKg()).append(" kg\n"));
        return sb.toString();
    }
}
