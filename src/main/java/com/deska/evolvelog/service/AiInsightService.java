package com.deska.evolvelog.service;

import com.deska.evolvelog.ai.provider.AiMessage;
import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiResponse;
import com.deska.evolvelog.ai.prompt.PromptLoader;
import com.deska.evolvelog.ai.provider.anthropic.ClaudeRequestContext;
import com.deska.evolvelog.ai.router.AiTaskType;
import com.deska.evolvelog.ai.router.ModelRouter;
import com.deska.evolvelog.ai.tools.AiToolRegistry;
import com.deska.evolvelog.domain.AiInsight;
import com.deska.evolvelog.repository.AiInsightRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AiInsightService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightService.class);

    private final AiProvider aiProvider;
    private final AiInsightRepository aiInsightRepository;
    private final AiSettingsService aiSettingsService;
    private final PromptLoader promptLoader;
    private final ModelRouter modelRouter;
    private final AiToolRegistry aiToolRegistry;
    private final ObjectMapper objectMapper;

    public AiInsightService(AiProvider aiProvider,
                            AiInsightRepository aiInsightRepository,
                            AiSettingsService aiSettingsService,
                            PromptLoader promptLoader,
                            ModelRouter modelRouter,
                            AiToolRegistry aiToolRegistry,
                            ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.aiInsightRepository = aiInsightRepository;
        this.aiSettingsService = aiSettingsService;
        this.promptLoader = promptLoader;
        this.modelRouter = modelRouter;
        this.aiToolRegistry = aiToolRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<AiInsight> listInsights(UUID userId, String type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (type != null && !type.isBlank()) {
            return aiInsightRepository.findByUserIdAndTypeOrderByGeneratedAtDesc(userId, type, pageable);
        }
        return aiInsightRepository.findByUserIdOrderByGeneratedAtDesc(userId, pageable);
    }

    @Transactional
    public AiInsight generateInsight(UUID userId, AiTaskType taskType) {
        String apiKey = aiSettingsService.getDecryptedApiKey(userId)
                .filter(k -> !k.isBlank())
                .orElseThrow(() -> new IllegalStateException("No API key configured"));

        try {
            ClaudeRequestContext.setApiKey(apiKey);
            return doGenerateInsight(userId, taskType);
        } finally {
            ClaudeRequestContext.clear();
        }
    }

    private AiInsight doGenerateInsight(UUID userId, AiTaskType taskType) {
        String modelId = modelRouter.selectModel(taskType);
        String systemPrompt = promptLoader.getPrompt(taskType);

        // Gather data using tools relevant to this task type
        String contextData = gatherContextForTaskType(userId, taskType);

        AiRequest request = AiRequest.builder()
                .modelId(modelId)
                .systemPrompt(systemPrompt)
                .messages(List.of(AiMessage.user(contextData)))
                .temperature(0.7)
                .build();

        AiResponse response = aiProvider.complete(request);

        LocalDate today = LocalDate.now();
        LocalDate periodStart = periodStartFor(taskType, today);

        AiInsight insight = AiInsight.builder()
                .userId(userId)
                .type(taskType.name())
                .periodStart(periodStart)
                .periodEnd(today)
                .content(response.content())
                .modelUsed(modelId)
                .build();

        return aiInsightRepository.save(insight);
    }

    private String gatherContextForTaskType(UUID userId, AiTaskType taskType) {
        ObjectNode emptyInput = objectMapper.createObjectNode();
        StringBuilder ctx = new StringBuilder();

        switch (taskType) {
            case DAILY_SUMMARY, POST_WORKOUT -> {
                aiToolRegistry.find("get_recent_workouts")
                        .ifPresent(t -> ctx.append(t.execute(inputWithLimit(1), userId)).append("\n"));
                aiToolRegistry.find("get_measurements")
                        .ifPresent(t -> ctx.append(t.execute(inputWithLimit(1), userId)).append("\n"));
            }
            case WEEKLY_REPORT -> {
                aiToolRegistry.find("get_recent_workouts")
                        .ifPresent(t -> ctx.append(t.execute(inputWithLimit(7), userId)).append("\n"));
                aiToolRegistry.find("get_personal_records")
                        .ifPresent(t -> ctx.append(t.execute(emptyInput, userId)).append("\n"));
            }
            case BLOOD_ANALYSIS -> {
                aiToolRegistry.find("get_blood_results")
                        .ifPresent(t -> ctx.append(t.execute(emptyInput, userId)).append("\n"));
            }
            default -> {
                aiToolRegistry.find("get_recent_workouts")
                        .ifPresent(t -> ctx.append(t.execute(inputWithLimit(5), userId)).append("\n"));
            }
        }

        return ctx.isEmpty() ? "No data available for this period." : ctx.toString();
    }

    private ObjectNode inputWithLimit(int limit) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("limit", limit);
        return node;
    }

    private LocalDate periodStartFor(AiTaskType taskType, LocalDate today) {
        return switch (taskType) {
            case DAILY_SUMMARY, POST_WORKOUT -> today;
            case WEEKLY_REPORT               -> today.minusDays(7);
            case BLOOD_ANALYSIS              -> today.minusMonths(3);
            case CHAT_CLASSIFY               -> today;
        };
    }
}
