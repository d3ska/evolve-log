package com.deska.evolvelog.ai.provider.gemini;

import com.deska.evolvelog.ai.provider.AiMessage;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiStreamSink;
import com.deska.evolvelog.ai.provider.AiToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiAdapterTest {

    private GeminiAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new GeminiAdapter(objectMapper);
    }

    // --- 9.1: Request serialisation ---

    @Test
    void shouldIncludeSystemInstructionWhenSystemPromptIsSet() throws Exception {
        // given
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .systemPrompt("You are a fitness trainer.")
                .messages(List.of(AiMessage.user("Hello")))
                .build();

        // when
        String body = adapter.buildRequestBody(request);
        JsonNode root = objectMapper.readTree(body);

        // then
        assertThat(root.has("systemInstruction")).isTrue();
        String text = root.path("systemInstruction").path("parts").path(0).path("text").asText();
        assertThat(text).isEqualTo("You are a fitness trainer.");
    }

    @Test
    void shouldRemapAssistantRoleToModel() throws Exception {
        // given
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .messages(List.of(
                        AiMessage.user("What is my PR?"),
                        AiMessage.assistant("Your squat PR is 150 kg.")
                ))
                .build();

        // when
        String body = adapter.buildRequestBody(request);
        JsonNode root = objectMapper.readTree(body);
        JsonNode contents = root.path("contents");

        // then
        assertThat(contents.get(0).path("role").asText()).isEqualTo("user");
        assertThat(contents.get(1).path("role").asText()).isEqualTo("model");
    }

    @Test
    void shouldProduceFunctionDeclarationsWithUppercasedTypes() throws Exception {
        // given
        Map<String, Object> inputSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "limit", Map.of("type", "integer", "description", "max rows")
                )
        );
        AiToolDefinition tool = new AiToolDefinition("get_recent_workouts", "Fetch workouts", inputSchema);
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .messages(List.of(AiMessage.user("Show workouts")))
                .tools(List.of(tool))
                .build();

        // when
        String body = adapter.buildRequestBody(request);
        JsonNode root = objectMapper.readTree(body);

        // then
        JsonNode decl = root.path("tools").path(0).path("functionDeclarations").path(0);
        assertThat(decl.path("name").asText()).isEqualTo("get_recent_workouts");
        assertThat(decl.path("parameters").path("type").asText()).isEqualTo("OBJECT");
        assertThat(decl.path("parameters").path("properties").path("limit").path("type").asText())
                .isEqualTo("INTEGER");
    }

    @Test
    void shouldOmitSystemInstructionWhenBlank() throws Exception {
        // given
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .systemPrompt("")
                .messages(List.of(AiMessage.user("Hi")))
                .build();

        // when
        String body = adapter.buildRequestBody(request);
        JsonNode root = objectMapper.readTree(body);

        // then
        assertThat(root.has("systemInstruction")).isFalse();
    }

    @Test
    void shouldOmitToolsWhenEmpty() throws Exception {
        // given
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .messages(List.of(AiMessage.user("Hi")))
                .build();

        // when
        String body = adapter.buildRequestBody(request);
        JsonNode root = objectMapper.readTree(body);

        // then
        assertThat(root.has("tools")).isFalse();
    }

    // --- 9.2: SSE parsing ---

    @Test
    void shouldEmitOnTokenForTextPart() {
        // given
        String sseData = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello athlete\"}]}}]}";
        CapturingSink sink = new CapturingSink();

        // when
        adapter.parseSseLine(sseData, sink);

        // then
        assertThat(sink.tokens).containsExactly("Hello athlete");
        assertThat(sink.toolUseNames).isEmpty();
    }

    @Test
    void shouldEmitOnToolUseForFunctionCallPart() {
        // given
        String sseData = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"functionCall\":{\"name\":\"get_personal_records\",\"args\":{\"limit\":5}}}]}}]}";
        CapturingSink sink = new CapturingSink();

        // when
        adapter.parseSseLine(sseData, sink);

        // then
        assertThat(sink.toolUseNames).containsExactly("get_personal_records");
        assertThat(sink.toolUseArgs).hasSize(1);
        assertThat(sink.toolUseArgs.get(0)).containsEntry("limit", 5);
        assertThat(sink.tokens).isEmpty();
    }

    @Test
    void shouldIgnoreNonDataLines() {
        // given
        CapturingSink sink = new CapturingSink();

        // when
        adapter.parseSseLine("", sink);
        adapter.parseSseLine(": keep-alive", sink);
        adapter.parseSseLine("event: message", sink);

        // then
        assertThat(sink.tokens).isEmpty();
        assertThat(sink.toolUseNames).isEmpty();
    }

    @Test
    void shouldIgnoreDoneMarker() {
        // given
        CapturingSink sink = new CapturingSink();

        // when
        adapter.parseSseLine("data: [DONE]", sink);

        // then
        assertThat(sink.tokens).isEmpty();
    }

    // --- 9.3: Missing API key guard ---

    @Test
    void shouldThrowIllegalStateExceptionWhenApiKeyMissingOnStream() {
        // given — no GeminiRequestContext.setApiKey() called
        GeminiRequestContext.clear();
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .messages(List.of(AiMessage.user("Hi")))
                .build();
        CapturingSink sink = new CapturingSink();

        // when / then
        assertThatThrownBy(() -> adapter.stream(request, sink))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No Gemini API key");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenApiKeyMissingOnComplete() {
        // given
        GeminiRequestContext.clear();
        AiRequest request = AiRequest.builder()
                .modelId("gemini-2.0-flash")
                .messages(List.of(AiMessage.user("Hi")))
                .build();

        // when / then
        assertThatThrownBy(() -> adapter.complete(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No Gemini API key");
    }

    // --- Helper sink ---

    private static class CapturingSink implements AiStreamSink {
        final List<String> tokens = new ArrayList<>();
        final List<String> toolUseNames = new ArrayList<>();
        final List<Map<String, Object>> toolUseArgs = new ArrayList<>();

        @Override public void onToken(String text) { tokens.add(text); }
        @Override public void onToolUse(String toolName, Map<String, Object> input) {
            toolUseNames.add(toolName);
            toolUseArgs.add(input);
        }
        @Override public void onToolResult(String toolName, int rowCount) {}
        @Override public void onDone() {}
        @Override public void onError(Throwable cause) {}
    }
}
