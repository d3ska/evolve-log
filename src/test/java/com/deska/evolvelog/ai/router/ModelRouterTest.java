package com.deska.evolvelog.ai.router;

import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiResponse;
import com.deska.evolvelog.ai.provider.ModelTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelRouterTest {

    @Mock private AiProvider claudeProvider;
    @Mock private AiProvider geminiProvider;

    private ModelRouter router;

    @BeforeEach
    void setUp() {
        lenient().when(claudeProvider.providerId()).thenReturn("anthropic");
        lenient().when(geminiProvider.providerId()).thenReturn("google");
        lenient().when(claudeProvider.modelIdForTier(ModelTier.FAST)).thenReturn("claude-haiku-4-5-20251001");
        router = new ModelRouter(List.of(claudeProvider, geminiProvider));
    }

    // --- 9.4: resolveProvider ---

    @Test
    void shouldResolveAnthropicProvider() {
        // when
        AiProvider resolved = router.resolveProvider("anthropic");

        // then
        assertThat(resolved).isSameAs(claudeProvider);
    }

    @Test
    void shouldResolveGoogleProvider() {
        // when
        AiProvider resolved = router.resolveProvider("google");

        // then
        assertThat(resolved).isSameAs(geminiProvider);
    }

    @Test
    void shouldThrowForUnknownProvider() {
        // when / then
        assertThatThrownBy(() -> router.resolveProvider("openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openai");
    }

    // --- 9.5: selectTier ---

    @Test
    void shouldReturnFastTierForDailySummary() {
        assertThat(router.selectTier(AiTaskType.DAILY_SUMMARY)).isEqualTo(ModelTier.FAST);
    }

    @Test
    void shouldReturnFastTierForPostWorkout() {
        assertThat(router.selectTier(AiTaskType.POST_WORKOUT)).isEqualTo(ModelTier.FAST);
    }

    @Test
    void shouldReturnFastTierForChatClassify() {
        assertThat(router.selectTier(AiTaskType.CHAT_CLASSIFY)).isEqualTo(ModelTier.FAST);
    }

    @Test
    void shouldReturnBalancedTierForWeeklyReport() {
        assertThat(router.selectTier(AiTaskType.WEEKLY_REPORT)).isEqualTo(ModelTier.BALANCED);
    }

    @Test
    void shouldReturnSmartTierForBloodAnalysis() {
        assertThat(router.selectTier(AiTaskType.BLOOD_ANALYSIS)).isEqualTo(ModelTier.SMART);
    }

    // --- 9.6: selectTierForChat heuristic paths ---

    @Test
    void shouldReturnFastTierForShortSimpleMessage() {
        // given — short message, no keywords → SIMPLE heuristic → FAST
        ModelTier tier = router.selectTierForChat("What did I lift yesterday?", "anthropic");

        assertThat(tier).isEqualTo(ModelTier.FAST);
    }

    @Test
    void shouldReturnSmartTierForMedicalKeyword() {
        // given — "blood" triggers MEDICAL heuristic → SMART
        ModelTier tier = router.selectTierForChat("What do my blood test results say?", "anthropic");

        assertThat(tier).isEqualTo(ModelTier.SMART);
    }

    @Test
    void shouldReturnBalancedTierForAnalyticalKeyword() {
        // given — "progress" triggers ANALYTICAL heuristic → BALANCED
        ModelTier tier = router.selectTierForChat("Show me my progress over the last month", "anthropic");

        assertThat(tier).isEqualTo(ModelTier.BALANCED);
    }

    @Test
    void shouldFallBackToLlmClassifierForAmbiguousMessage() {
        // given — long message with no keywords triggers LLM fallback
        String longAmbiguousMessage = "I have been going to the gym regularly and eating well, " +
                "can you help me figure out the best way to structure my next session?";
        when(claudeProvider.complete(any(AiRequest.class)))
                .thenReturn(new AiResponse("ANALYTICAL", 10, 5));

        // when
        ModelTier tier = router.selectTierForChat(longAmbiguousMessage, "anthropic");

        // then
        assertThat(tier).isEqualTo(ModelTier.BALANCED);
    }

    @Test
    void shouldDefaultToFastWhenLlmClassifierFails() {
        // given — long ambiguous message, LLM throws
        String longAmbiguousMessage = "I have been going to the gym regularly and eating well, " +
                "can you help me figure out the best way to structure my next session?";
        when(claudeProvider.complete(any(AiRequest.class)))
                .thenThrow(new RuntimeException("network error"));

        // when
        ModelTier tier = router.selectTierForChat(longAmbiguousMessage, "anthropic");

        // then — falls back to SIMPLE → FAST
        assertThat(tier).isEqualTo(ModelTier.FAST);
    }
}
