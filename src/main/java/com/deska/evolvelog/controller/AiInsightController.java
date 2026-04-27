package com.deska.evolvelog.controller;

import com.deska.evolvelog.ai.router.AiTaskType;
import com.deska.evolvelog.domain.AiInsight;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.service.AiInsightService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/insights")
public class AiInsightController {

    private final AiInsightService aiInsightService;

    public AiInsightController(AiInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listInsights(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AiInsight> insights = aiInsightService.listInsights(user.getId(), type, page, size);
        List<Map<String, Object>> dtos = insights.getContent().stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.paged(dtos, insights.getTotalElements(), page, size));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateInsight(
            @AuthenticationPrincipal User user,
            @RequestParam String type) {

        AiTaskType taskType = AiTaskType.valueOf(type.toUpperCase());
        AiInsight insight = aiInsightService.generateInsight(user.getId(), taskType);
        return ResponseEntity.ok(ApiResponse.success(toMap(insight)));
    }

    private Map<String, Object> toMap(AiInsight insight) {
        return Map.of(
                "id", insight.getId(),
                "type", insight.getType(),
                "periodStart", insight.getPeriodStart(),
                "periodEnd", insight.getPeriodEnd(),
                "content", insight.getContent(),
                "modelUsed", insight.getModelUsed(),
                "generatedAt", insight.getGeneratedAt()
        );
    }
}
