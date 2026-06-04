package com.deska.evolvelog.service;

import com.deska.evolvelog.ai.prompt.PromptContextBuilder;
import com.deska.evolvelog.ai.prompt.PromptLoader;
import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiStreamSink;
import com.deska.evolvelog.ai.provider.ModelTier;
import com.deska.evolvelog.ai.router.ModelRouter;
import com.deska.evolvelog.ai.tools.AiTool;
import com.deska.evolvelog.ai.tools.AiToolRegistry;
import com.deska.evolvelog.domain.AiChatMessage;
import com.deska.evolvelog.repository.AiChatHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock private AiChatHistoryRepository chatHistoryRepository;
    @Mock private AiSettingsService aiSettingsService;
    @Mock private PromptLoader promptLoader;
    @Mock private PromptContextBuilder promptContextBuilder;
    @Mock private ModelRouter modelRouter;
    @Mock private AiToolRegistry aiToolRegistry;
    @Mock private AiProvider provider;

    private AiChatService service;
    private UUID userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        service = new AiChatService(chatHistoryRepository, aiSettingsService,
                promptLoader, promptContextBuilder, modelRouter, aiToolRegistry, new ObjectMapper());
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        lenient().when(chatHistoryRepository.findTop10ByUserIdAndConversationIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        lenient().when(promptLoader.getChatPrompt()).thenReturn("You are a trainer. {{context}}");
        lenient().when(promptContextBuilder.buildContext(any(), any())).thenReturn("Context data");
        lenient().when(aiSettingsService.getProvider(any())).thenReturn("anthropic");
        lenient().when(modelRouter.resolveProvider("anthropic")).thenReturn(provider);
        lenient().when(modelRouter.selectTierForChat(any(), any())).thenReturn(ModelTier.FAST);
        lenient().when(provider.providerId()).thenReturn("anthropic");
        lenient().when(provider.modelIdForTier(ModelTier.FAST)).thenReturn("claude-haiku-4-5-20251001");
        lenient().when(aiToolRegistry.toDefinitions()).thenReturn(List.of());
        lenient().when(chatHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldStreamTokensAndPersistMessagesWhenApiKeyPresent() {
        // given
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("sk-ant-valid-key"));
        doAnswer(inv -> {
            AiStreamSink sink = inv.getArgument(1);
            sink.onToken("Hello");
            sink.onToken(", athlete!");
            sink.onDone();
            return null;
        }).when(provider).stream(any(), any());

        CapturingSink sink = new CapturingSink();

        // when
        service.chat(userId, conversationId, "What is my PR?", "progress", sink);

        // then
        assertThat(sink.tokens).containsExactly("Hello", ", athlete!");
        assertThat(sink.done).isTrue();

        ArgumentCaptor<AiChatMessage> captor = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(chatHistoryRepository, atLeast(1)).save(captor.capture());
        List<AiChatMessage> saved = captor.getAllValues();
        assertThat(saved).anyMatch(m -> "user".equals(m.getRole()) && m.getContent().equals("What is my PR?"));
        assertThat(saved).anyMatch(m -> "assistant".equals(m.getRole()) && m.getContent().equals("Hello, athlete!"));
    }

    @Test
    void shouldCallPrepareContextAndClearContextOnProvider() {
        // given
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("sk-ant-valid-key"));
        doAnswer(inv -> {
            AiStreamSink sink = inv.getArgument(1);
            sink.onToken("Hi");
            sink.onDone();
            return null;
        }).when(provider).stream(any(), any());

        // when
        service.chat(userId, conversationId, "Hello", null, new CapturingSink());

        // then
        verify(provider).prepareContext("sk-ant-valid-key");
        verify(provider).clearContext();
    }

    @Test
    void shouldEmitErrorEventWhenApiKeyMissing() {
        // given
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.empty());
        CapturingSink sink = new CapturingSink();

        // when
        service.chat(userId, conversationId, "What is my PR?", null, sink);

        // then
        assertThat(sink.error).isNotNull();
        assertThat(sink.error.getMessage()).contains("No API key");
        verify(provider, never()).stream(any(), any());
    }

    @Test
    void shouldClearContextEvenWhenStreamThrows() {
        // given
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("sk-ant-valid-key"));
        doThrow(new RuntimeException("network error")).when(provider).stream(any(), any());

        // when
        service.chat(userId, conversationId, "Hello", null, new CapturingSink());

        // then
        verify(provider).clearContext();
    }

    @Test
    void shouldExecuteToolAndInjectResultThenContinueStreaming() {
        // given
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("sk-ant-key"));

        AiTool fakeTool = mock(AiTool.class);
        lenient().when(fakeTool.name()).thenReturn("get_personal_records");
        when(fakeTool.execute(any(), eq(userId))).thenReturn("Squat: 150kg");
        when(aiToolRegistry.find("get_personal_records")).thenReturn(Optional.of(fakeTool));

        doAnswer(inv -> {
            AiStreamSink sink = inv.getArgument(1);
            sink.onToolUse("get_personal_records", Map.of()); // phase 1
            sink.onToolUse("get_personal_records", Map.of()); // phase 2
            sink.onDone();
            return null;
        }).doAnswer(inv -> {
            AiStreamSink sink = inv.getArgument(1);
            sink.onToken("Your squat PR is 150kg.");
            sink.onDone();
            return null;
        }).when(provider).stream(any(), any());

        CapturingSink sink = new CapturingSink();

        // when
        service.chat(userId, conversationId, "What are my PRs?", "progress", sink);

        // then
        assertThat(sink.toolUseNames).contains("get_personal_records");
        assertThat(sink.toolResultNames).contains("get_personal_records");
        assertThat(sink.tokens).containsExactly("Your squat PR is 150kg.");
        assertThat(sink.done).isTrue();
        verify(fakeTool).execute(any(), eq(userId));
        verify(provider, times(2)).stream(any(), any());
    }

    // --- Helper sink ---

    private static class CapturingSink implements AiStreamSink {
        final List<String> tokens = new java.util.ArrayList<>();
        final List<String> toolUseNames = new java.util.ArrayList<>();
        final List<String> toolResultNames = new java.util.ArrayList<>();
        boolean done = false;
        Throwable error = null;

        @Override public void onToken(String text) { tokens.add(text); }
        @Override public void onToolUse(String toolName, Map<String, Object> input) { toolUseNames.add(toolName); }
        @Override public void onToolResult(String toolName, int rowCount) { toolResultNames.add(toolName); }
        @Override public void onDone() { done = true; }
        @Override public void onError(Throwable cause) { error = cause; }
    }
}
