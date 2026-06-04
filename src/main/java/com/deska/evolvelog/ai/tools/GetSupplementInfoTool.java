package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.SupplementPlan;
import com.deska.evolvelog.domain.SupplementPlanEntry;
import com.deska.evolvelog.domain.TimeSlot;
import com.deska.evolvelog.repository.SupplementPlanRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetSupplementInfoTool implements AiTool {

    private final SupplementPlanRepository supplementPlanRepository;

    public GetSupplementInfoTool(SupplementPlanRepository supplementPlanRepository) {
        this.supplementPlanRepository = supplementPlanRepository;
    }

    @Override
    public String name() {
        return "get_supplement_info";
    }

    @Override
    public String description() {
        return "Returns the athlete's supplement plans with all scheduled supplements, doses, timing, " +
               "and notes. Use this when asked about supplement protocol, nutrient timing, recovery " +
               "support, or when cross-referencing diet and supplementation.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "active_only", Map.of(
                                "type", "boolean",
                                "description", "If true (default), return only active supplement plans."
                        )
                ),
                "required", List.of()
        );
    }

    @Override
    public String execute(ObjectNode input, UUID userId) {
        boolean activeOnly = input.path("active_only").asBoolean(true);

        List<SupplementPlan> plans = supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<SupplementPlan> filtered = plans.stream()
                .filter(p -> !activeOnly || p.isActive())
                .toList();

        if (filtered.isEmpty()) {
            if (activeOnly && !plans.isEmpty()) {
                return "No active supplement plans found. Use active_only: false to see all plans.";
            }
            return "No supplement plans found.";
        }

        String label = activeOnly ? "active" : "all";
        StringBuilder sb = new StringBuilder("Supplement plans (").append(label).append("):\n\n");

        for (SupplementPlan plan : filtered) {
            sb.append("### ").append(plan.getName());
            if (!plan.isActive()) sb.append(" [inactive]");
            sb.append("\n");
            if (plan.getDescription() != null && !plan.getDescription().isBlank()) {
                sb.append("*").append(plan.getDescription()).append("*\n");
            }

            List<SupplementPlanEntry> entries = plan.getEntries();
            if (entries.isEmpty()) {
                sb.append("  (no supplements defined)\n");
            } else {
                for (SupplementPlanEntry entry : entries) {
                    sb.append("- ").append(entry.getSupplement().getName());
                    if (entry.getSupplement().getForm() != null) {
                        sb.append(" (").append(entry.getSupplement().getForm()).append(")");
                    }
                    if (entry.getDoseAmount() != null) {
                        sb.append(" — ").append(entry.getDoseAmount().stripTrailingZeros().toPlainString());
                        if (entry.getDoseUnit() != null) sb.append(" ").append(entry.getDoseUnit());
                    }
                    sb.append(", ");
                    if (entry.getTimeSlot() == TimeSlot.CUSTOM && entry.getCustomTime() != null) {
                        sb.append(entry.getCustomTime());
                    } else {
                        sb.append(entry.getTimeSlot().name());
                    }
                    if (entry.getNotes() != null && !entry.getNotes().isBlank()) {
                        sb.append(", notes: ").append(entry.getNotes());
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString().stripTrailing();
    }
}
