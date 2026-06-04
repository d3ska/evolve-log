package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.PlannedExercise;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetTrainingPlanTool implements AiTool {

    private final TrainingPlanRepository trainingPlanRepository;

    public GetTrainingPlanTool(TrainingPlanRepository trainingPlanRepository) {
        this.trainingPlanRepository = trainingPlanRepository;
    }

    @Override
    public String name() {
        return "get_training_plan";
    }

    @Override
    public String description() {
        return "Returns the athlete's training plans with all planned exercises, sets, rep ranges, " +
               "rest periods, and notes. Use this whenever the user asks about their training program, " +
               "wants to modify a plan, or when comparing planned vs actual performance.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "active_only", Map.of(
                                "type", "boolean",
                                "description", "If true (default), return only active plans. Set false to include inactive/archived plans."
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        boolean activeOnly = input.path("active_only").asBoolean(true);

        List<TrainingPlan> plans = trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId);

        List<TrainingPlan> filtered = plans.stream()
                .filter(p -> !activeOnly || p.isActive())
                .toList();

        if (filtered.isEmpty()) {
            if (activeOnly && !plans.isEmpty()) {
                return "No active training plans found. Use active_only: false to see all plans.";
            }
            return "No training plans found.";
        }

        String label = activeOnly ? "active" : "all";
        StringBuilder sb = new StringBuilder("Training plans (").append(label).append("):\n\n");

        for (TrainingPlan plan : filtered) {
            sb.append("### ").append(plan.getName());
            if (!plan.isActive()) sb.append(" [inactive]");
            if (plan.getDayOfWeek() != null) sb.append(" (").append(plan.getDayOfWeek()).append(")");
            if (plan.getBlock() != null) sb.append(" [Block: ").append(plan.getBlock().getName()).append("]");
            sb.append("\n");

            List<PlannedExercise> exercises = plan.getPlannedExercises();
            if (exercises.isEmpty()) {
                sb.append("  (no exercises defined)\n");
            } else {
                for (PlannedExercise ex : exercises) {
                    sb.append(ex.getPosition()).append(". ").append(ex.getName()).append(" — ");
                    sb.append(ex.getSets()).append("×");
                    if (ex.getRepsMin().equals(ex.getRepsMax())) {
                        sb.append(ex.getRepsMin());
                    } else {
                        sb.append(ex.getRepsMin()).append("-").append(ex.getRepsMax());
                    }
                    if (ex.getRestSeconds() != null) {
                        sb.append(", rest ").append(ex.getRestSeconds()).append("s");
                    }
                    if (ex.getNotes() != null && !ex.getNotes().isBlank()) {
                        sb.append(", notes: ").append(ex.getNotes());
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString().stripTrailing();
    }
}
