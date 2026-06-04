package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetRecentWorkoutsTool implements AiTool {

    private final WorkoutSessionRepository workoutSessionRepository;

    public GetRecentWorkoutsTool(WorkoutSessionRepository workoutSessionRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
    }

    @Override
    public String name() {
        return "get_recent_workouts";
    }

    @Override
    public String description() {
        return "Returns the athlete's most recent workout sessions with their exercises, sets, reps, and weights.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of(
                                "type", "integer",
                                "description", "Number of recent sessions to return (default 5, max 20)"
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        int limit = Math.min(input.path("limit").asInt(5), 20);
        List<WorkoutSession> sessions = workoutSessionRepository
                .findRecentByUserIdWithExercises(userId, PageRequest.of(0, limit));

        if (sessions.isEmpty()) {
            return "No workout sessions found.";
        }
        StringBuilder sb = new StringBuilder("Recent workouts:\n");
        sessions.forEach(session -> {
            sb.append("## ").append(session.getDate().toLocalDate());
            if (session.getDurationMinutes() != null) {
                sb.append(" (").append(session.getDurationMinutes()).append(" min)");
            }
            sb.append("\n");
            session.getExercises().forEach(e -> {
                if (e.getReps() != null || e.getWeightKg() != null) {
                    // Legacy aggregate path (pre-V17 or manually set aggregate)
                    sb.append("- ").append(e.getName())
                            .append(": ").append(e.getSets()).append("×");
                    if (e.getReps() != null) sb.append(e.getReps());
                    if (e.getWeightKg() != null) sb.append(" @ ").append(e.getWeightKg()).append(" kg");
                    if (e.getRpe() != null) sb.append(" [RPE ").append(e.getRpe()).append("]");
                    sb.append("\n");
                } else if (!e.getWorkoutSets().isEmpty()) {
                    // Modern per-set path (V17+): workoutSets are EAGER loaded
                    sb.append("- ").append(e.getName()).append(" (").append(e.getSets()).append(" sets)");
                    if (e.getRpe() != null) sb.append(" [RPE ").append(e.getRpe()).append("]");
                    sb.append(":\n");
                    List<WorkoutSet> sets = e.getWorkoutSets();
                    for (WorkoutSet ws : sets) {
                        sb.append("  set ").append(ws.getSetNumber()).append(": ");
                        if (ws.isCompleted()) {
                            if (ws.getReps() != null) sb.append(ws.getReps()).append(" reps");
                            if (ws.getWeightKg() != null) sb.append(" @ ").append(ws.getWeightKg()).append(" kg");
                        } else {
                            sb.append("planned (not completed)");
                        }
                        sb.append("\n");
                    }
                } else {
                    sb.append("- ").append(e.getName())
                            .append(": ").append(e.getSets()).append(" sets (no rep/weight data)");
                    if (e.getRpe() != null) sb.append(" [RPE ").append(e.getRpe()).append("]");
                    sb.append("\n");
                }
            });
        });
        return sb.toString();
    }
}
