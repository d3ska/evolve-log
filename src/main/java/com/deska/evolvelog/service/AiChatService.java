package com.deska.evolvelog.service;

import com.deska.evolvelog.ai.provider.AiMessage;
import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiStreamSink;
import com.deska.evolvelog.ai.prompt.PromptContextBuilder;
import com.deska.evolvelog.ai.prompt.PromptLoader;
import com.deska.evolvelog.ai.provider.anthropic.ClaudeRequestContext;
import com.deska.evolvelog.ai.router.ModelRouter;
import com.deska.evolvelog.ai.tools.AiTool;
import com.deska.evolvelog.ai.tools.AiToolRegistry;
import com.deska.evolvelog.domain.AiChatMessage;
import com.deska.evolvelog.repository.AiChatHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_TOOL_CALLS = 5;

    private final AiProvider aiProvider;
    private final AiChatHistoryRepository chatHistoryRepository;
    private final AiSettingsService aiSettingsService;
    private final PromptLoader promptLoader;
    private final PromptContextBuilder promptContextBuilder;
    private final ModelRouter modelRouter;
    private final AiToolRegistry aiToolRegistry;
    private final ObjectMapper objectMapper;

    public AiChatService(AiProvider aiProvider,
                         AiChatHistoryRepository chatHistoryRepository,
                         AiSettingsService aiSettingsService,
                         PromptLoader promptLoader,
                         PromptContextBuilder promptContextBuilder,
                         ModelRouter modelRouter,
                         AiToolRegistry aiToolRegistry,
                         ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.chatHistoryRepository = chatHistoryRepository;
        this.aiSettingsService = aiSettingsService;
        this.promptLoader = promptLoader;
        this.promptContextBuilder = promptContextBuilder;
        this.modelRouter = modelRouter;
        this.aiToolRegistry = aiToolRegistry;
        this.objectMapper = objectMapper;
    }

    public void chat(UUID userId, UUID conversationId, String userMessage,
                     String pageContext, AiStreamSink sink) {
        String apiKey = aiSettingsService.getDecryptedApiKey(userId)
                .filter(k -> !k.isBlank())
                .orElse(null);

        if (apiKey == null) {
            sink.onError(new IllegalStateException("No API key configured. Please add your API key in Settings."));
            return;
        }

        try {
            ClaudeRequestContext.setApiKey(apiKey);
            doChat(userId, conversationId, userMessage, pageContext, sink);
        } catch (Exception e) {
            log.error("Chat error for user {}", userId, e);
            sink.onError(new RuntimeException(userFacingError(e)));
        } finally {
            ClaudeRequestContext.clear();
        }
    }

    private void doChat(UUID userId, UUID conversationId, String userMessage,
                        String pageContext, AiStreamSink sink) {
        // Fetch last 10 messages (newest first), reverse to oldest-first for context
        List<AiChatMessage> history = chatHistoryRepository
                .findTop10ByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId);

        List<AiMessage> messages = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatMessage msg = history.get(i);
            // Skip consecutive messages with the same role — Anthropic rejects them with HTTP 400
            if (!messages.isEmpty() && messages.getLast().role().equals(msg.getRole())) {
                log.warn("Skipping consecutive {} message in history for user {} — possible orphan from failed request",
                        msg.getRole(), userId);
                continue;
            }
            messages.add(new AiMessage(msg.getRole(), msg.getContent()));
        }
        messages.add(AiMessage.user(userMessage));

        // Build system prompt with page context data
        String contextData = promptContextBuilder.buildContext(pageContext, userId);
        String systemPrompt = promptLoader.getChatPrompt()
                .replace("{{context}}", contextData != null ? contextData : "No additional context available.");

        String modelId = modelRouter.selectModelForChat(userMessage);

        // Tool call loop — streams until no tools are used or limit is reached
        StringBuilder assistantContent = new StringBuilder();
        int toolCallCount = 0;

        while (true) {
            AiRequest request = AiRequest.builder()
                    .modelId(modelId)
                    .systemPrompt(systemPrompt)
                    .messages(List.copyOf(messages))
                    .tools(aiToolRegistry.toDefinitions())
                    .temperature(0.7)
                    .build();

            List<ToolCall> toolCalls = new ArrayList<>();
            CollectingStreamSink collecting = new CollectingStreamSink(sink, toolCalls, assistantContent);

            aiProvider.stream(request, collecting);

            if (toolCalls.isEmpty() || toolCallCount >= MAX_TOOL_CALLS) {
                break;
            }

            toolCallCount += toolCalls.size();

            // Add accumulated assistant text to context before tool results
            if (!assistantContent.isEmpty()) {
                messages.add(AiMessage.assistant(assistantContent.toString()));
                assistantContent.setLength(0);
            }

            // Execute each tool and inject result into conversation
            for (ToolCall tc : toolCalls) {
                ObjectNode inputNode = objectMapper.convertValue(tc.input(), ObjectNode.class);
                String result = aiToolRegistry.find(tc.name())
                        .map(tool -> tool.execute(inputNode, userId))
                        .orElse("Tool not found: " + tc.name());

                int rowCount = countNonBlankLines(result);
                sink.onToolResult(tc.name(), rowCount);

                messages.add(AiMessage.user("Tool result for " + tc.name() + ":\n" + result));
            }
        }

        log.info("Claude raw response (userId={}, conversationId={}):\n---BEGIN---\n{}\n---END---",
                userId, conversationId, assistantContent);

        // Persist both messages only when the AI responded successfully.
        // If the AI call failed (empty response), skip persistence so history stays clean —
        // orphaned user messages with no assistant reply would corrupt future requests.
        String finalContent = assistantContent.toString();
        if (!finalContent.isBlank()) {
            chatHistoryRepository.save(AiChatMessage.builder()
                    .userId(userId)
                    .conversationId(conversationId)
                    .role("user")
                    .content(userMessage)
                    .build());
            chatHistoryRepository.save(AiChatMessage.builder()
                    .userId(userId)
                    .conversationId(conversationId)
                    .role("assistant")
                    .content(finalContent)
                    .build());
        }

        sink.onDone();
    }

    private String userFacingError(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("No API key")) {
            return "No API key configured. Please add your Anthropic API key in Settings.";
        }
        if (msg != null && msg.contains("HTTP 429")) {
            return "The AI service is currently overloaded. Please wait a moment and try again.";
        }
        if (msg != null && (msg.contains("HTTP 4") || msg.contains("HTTP 5"))) {
            return "The AI service returned an error. Please try again shortly.";
        }
        return "Something went wrong. Please try again.";
    }

    private int countNonBlankLines(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) text.lines().filter(l -> !l.isBlank()).count();
    }

    /**
     * Intercepts {@code onDone} from the provider to prevent premature stream completion
     * during a tool-call loop. Collects tool use events in two-phase order:
     * phase 1 = start notification (forwarded to delegate for UI), phase 2 = full input (collected for execution).
     */
    private static class CollectingStreamSink implements AiStreamSink {

        private final AiStreamSink delegate;
        private final List<ToolCall> toolCalls;
        private final StringBuilder assistantContent;
        private String pendingToolName = null;

        CollectingStreamSink(AiStreamSink delegate, List<ToolCall> toolCalls,
                             StringBuilder assistantContent) {
            this.delegate = delegate;
            this.toolCalls = toolCalls;
            this.assistantContent = assistantContent;
        }

        @Override
        public void onToken(String text) {
            assistantContent.append(text);
            delegate.onToken(text);
        }

        @Override
        public void onToolUse(String toolName, Map<String, Object> input) {
            if (pendingToolName == null) {
                // Phase 1: start notification — forward to UI, record pending
                pendingToolName = toolName;
                delegate.onToolUse(toolName, input);
            } else {
                // Phase 2: full input arrived — collect for execution, reset pending
                toolCalls.add(new ToolCall(pendingToolName, input));
                pendingToolName = null;
            }
        }

        @Override
        public void onToolResult(String toolName, int rowCount) {
            delegate.onToolResult(toolName, rowCount);
        }

        @Override
        public void onDone() {
            // Suppress — AiChatService.doChat() calls sink.onDone() after the loop
        }

        @Override
        public void onError(Throwable cause) {
            delegate.onError(cause);
        }
    }

    private record ToolCall(String name, Map<String, Object> input) {}
}
