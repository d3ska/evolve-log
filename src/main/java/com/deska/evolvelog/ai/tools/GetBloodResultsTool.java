package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.BloodTestReport;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetBloodResultsTool implements AiTool {

    private final BloodTestReportRepository bloodTestReportRepository;

    public GetBloodResultsTool(BloodTestReportRepository bloodTestReportRepository) {
        this.bloodTestReportRepository = bloodTestReportRepository;
    }

    @Override
    public String name() {
        return "get_blood_results";
    }

    @Override
    public String description() {
        return "Returns all blood test reports for the athlete, including individual markers with values, " +
               "units, reference ranges, and any out-of-range flags.";
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
        List<BloodTestReport> reports = bloodTestReportRepository.findByUserIdOrderByDateDesc(userId);
        if (reports.isEmpty()) {
            return "No blood test results found.";
        }
        StringBuilder sb = new StringBuilder("Blood test reports:\n");
        reports.forEach(report -> {
            sb.append("## ").append(report.getDate());
            if (report.getLabName() != null) sb.append(" — ").append(report.getLabName());
            sb.append("\n");
            report.getResults().forEach(r -> {
                sb.append("- ").append(r.getParameterLabel())
                        .append(": ").append(r.getValue());
                if (r.getUnit() != null) sb.append(" ").append(r.getUnit());
                if (r.getRefLow() != null && r.getRefHigh() != null) {
                    sb.append(" [").append(r.getRefLow()).append("–").append(r.getRefHigh()).append("]");
                }
                if (r.getFlag() != null) sb.append(" ⚠ ").append(r.getFlag());
                sb.append("\n");
            });
        });
        return sb.toString();
    }
}
