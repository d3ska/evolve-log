package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.dto.response.PersonalRecordDto;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetPersonalRecordsTool implements AiTool {

    private final ExerciseRepository exerciseRepository;

    public GetPersonalRecordsTool(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    public String name() {
        return "get_personal_records";
    }

    @Override
    public String description() {
        return "Returns the athlete's all-time personal records (heaviest weight lifted) for every exercise they have logged.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        List<PersonalRecordDto> records = exerciseRepository.findPersonalRecordsByUserId(userId);
        if (records.isEmpty()) {
            return "No personal records found.";
        }
        StringBuilder sb = new StringBuilder("Personal Records:\n");
        records.forEach(r -> sb.append("- ").append(r.exerciseName())
                .append(": ").append(r.maxWeightKg()).append(" kg")
                .append(" (").append(r.setsAtMax()).append("×").append(r.repsAtMax()).append(")")
                .append(" on ").append(r.achievedAt().toLocalDate()).append("\n"));
        return sb.toString();
    }
}
