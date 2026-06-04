package com.deska.evolvelog.ai.router;

import com.deska.evolvelog.ai.provider.AiMessage;
import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiResponse;
import com.deska.evolvelog.ai.provider.ModelTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    private static final Set<String> ANALYTICAL_KEYWORDS = Set.of(
            "trend", "progress", "progression", "compare", "comparison", "volume",
            "last week", "last month", "weeks", "months", "overload", "peak",
            "average", "improved", "improvement"
    );

    private static final Set<String> MEDICAL_KEYWORDS = Set.of(
            "blood", "hrv", "fatigue", "health", "marker", "hemoglobin",
            "cholesterol", "testosterone", "cortisol", "vitamin", "ferritin",
            "creatinine", "audit", "hormone"
    );

    private final Map<String, AiProvider> providers;

    public ModelRouter(List<AiProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(AiProvider::providerId, Function.identity()));
    }

    public AiProvider resolveProvider(String providerId) {
        AiProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalStateException("Unknown AI provider: '" + providerId + "'");
        }
        return provider;
    }

    /** Deterministic tier selection for background scheduled tasks. */
    public ModelTier selectTier(AiTaskType taskType) {
        return switch (taskType) {
            case DAILY_SUMMARY, POST_WORKOUT, CHAT_CLASSIFY -> ModelTier.FAST;
            case WEEKLY_REPORT                              -> ModelTier.BALANCED;
            case BLOOD_ANALYSIS                             -> ModelTier.SMART;
        };
    }

    /**
     * Heuristic-first tier selection for chat messages.
     * Falls back to an LLM classifier when intent is ambiguous.
     */
    public ModelTier selectTierForChat(String message, String providerId) {
        ChatIntent intent = classifyByHeuristic(message);
        if (intent != null) {
            log.debug("Chat heuristic classified '{}...' as {}", truncate(message), intent);
            return tierForIntent(intent);
        }
        // Ambiguous — call the user's active provider as classifier
        log.debug("Chat heuristic inconclusive, using LLM classifier");
        ChatIntent classified = classifyByLlm(message, resolveProvider(providerId));
        return tierForIntent(classified);
    }

    ChatIntent classifyByHeuristic(String message) {
        if (message == null) return ChatIntent.SIMPLE;
        String lower = message.toLowerCase(Locale.ROOT);

        if (containsAny(lower, MEDICAL_KEYWORDS)) return ChatIntent.MEDICAL;
        if (containsAny(lower, ANALYTICAL_KEYWORDS)) return ChatIntent.ANALYTICAL;
        if (message.length() < 80) return ChatIntent.SIMPLE;

        return null; // ambiguous — let LLM decide
    }

    private ChatIntent classifyByLlm(String message, AiProvider provider) {
        try {
            String fastModelId = provider.modelIdForTier(ModelTier.FAST);
            AiRequest req = AiRequest.builder()
                    .modelId(fastModelId)
                    .systemPrompt("""
                            Classify the user's gym tracking question into exactly one category.
                            Reply with only: SIMPLE, ANALYTICAL, or MEDICAL
                            SIMPLE = short factual question about a single workout or exercise.
                            ANALYTICAL = trend analysis, multi-week comparison, volume, progression.
                            MEDICAL = blood tests, HRV, health markers, fatigue, hormones.
                            """)
                    .messages(List.of(AiMessage.user(message)))
                    .temperature(0.0)
                    .build();

            AiResponse response = provider.complete(req);
            String classification = response.content().trim().toUpperCase(Locale.ROOT);
            return switch (classification) {
                case "ANALYTICAL" -> ChatIntent.ANALYTICAL;
                case "MEDICAL"    -> ChatIntent.MEDICAL;
                default           -> ChatIntent.SIMPLE;
            };
        } catch (Exception e) {
            log.warn("LLM classifier failed, defaulting to SIMPLE", e);
            return ChatIntent.SIMPLE;
        }
    }

    private ModelTier tierForIntent(ChatIntent intent) {
        return switch (intent) {
            case SIMPLE     -> ModelTier.FAST;
            case ANALYTICAL -> ModelTier.BALANCED;
            case MEDICAL    -> ModelTier.SMART;
        };
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String truncate(String s) {
        return s.length() > 40 ? s.substring(0, 40) : s;
    }
}
