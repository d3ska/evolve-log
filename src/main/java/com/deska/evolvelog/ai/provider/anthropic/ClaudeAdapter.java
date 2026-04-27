package com.deska.evolvelog.ai.provider.anthropic;

import com.deska.evolvelog.ai.provider.AiMessage;
import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiResponse;
import com.deska.evolvelog.ai.provider.AiStreamSink;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ClaudeAdapter implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAdapter.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClaudeAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String providerId() {
        return "anthropic";
    }

    @Override
    public AiResponse complete(AiRequest request) {
        try {
            String body = buildRequestBody(request, false);
            HttpRequest httpRequest = buildHttpRequest(request, body);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ClaudeApiException("Anthropic API error: HTTP " + response.statusCode() + " — " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractTextContent(root);
            int inputTokens = root.path("usage").path("input_tokens").asInt(0);
            int outputTokens = root.path("usage").path("output_tokens").asInt(0);
            return new AiResponse(content, inputTokens, outputTokens);

        } catch (ClaudeApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ClaudeApiException("Failed to call Anthropic API", e);
        }
    }

    @Override
    public void stream(AiRequest request, AiStreamSink sink) {
        try {
            String body = buildRequestBody(request, true);
            HttpRequest httpRequest = buildHttpRequest(request, body);

            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                String errorBody = response.body().collect(Collectors.joining("\n"));
                log.error("Anthropic API error: HTTP {} — {}", response.statusCode(), errorBody);
                throw new ClaudeApiException("Anthropic API error: HTTP " + response.statusCode());
            }

            SseStreamParser parser = new SseStreamParser(sink, objectMapper);
            response.body().forEach(parser::processLine);
            if (!parser.isCompleted()) {
                sink.onDone();
            }

        } catch (ClaudeApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error streaming from Anthropic API", e);
            sink.onError(e);
        }
    }

    private HttpRequest buildHttpRequest(AiRequest request, String body) throws Exception {
        // API key is passed via request metadata — resolved by AiChatService before calling adapter
        // The convention: model ID prefix determines the header; key stored in thread-local or passed via closure
        // For now the key must be set via setApiKey() before each call — see ClaudeRequestContext
        String apiKey = ClaudeRequestContext.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ClaudeApiException("No Anthropic API key configured for this request");
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String buildRequestBody(AiRequest request, boolean stream) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", request.modelId());
            body.put("max_tokens", 4096);
            body.put("temperature", request.temperature());
            body.put("stream", stream);

            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                body.put("system", request.systemPrompt());
            }

            List<Map<String, Object>> messages = new ArrayList<>();
            for (AiMessage msg : request.messages()) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
            body.put("messages", messages);

            if (!request.tools().isEmpty()) {
                List<Map<String, Object>> tools = request.tools().stream()
                        .map(t -> Map.of(
                                "name", (Object) t.name(),
                                "description", t.description(),
                                "input_schema", t.inputSchema()
                        ))
                        .toList();
                body.put("tools", tools);
            }

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new ClaudeApiException("Failed to serialize request body", e);
        }
    }

    private String extractTextContent(JsonNode root) {
        JsonNode contentArray = root.path("content");
        if (contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText("");
                }
            }
        }
        return "";
    }

    /**
     * Parses the Anthropic SSE stream line by line.
     * Anthropic SSE format: alternating "event: <type>" and "data: <json>" lines.
     */
    static class SseStreamParser {

        private final AiStreamSink sink;
        private final ObjectMapper objectMapper;
        private String currentEvent = null;
        private String currentToolName = null;
        private final StringBuilder toolInputAccumulator = new StringBuilder();
        private boolean inToolUseBlock = false;
        private boolean completed = false;

        SseStreamParser(AiStreamSink sink, ObjectMapper objectMapper) {
            this.sink = sink;
            this.objectMapper = objectMapper;
        }

        void processLine(String line) {
            if (line.startsWith("event: ")) {
                currentEvent = line.substring(7).trim();
            } else if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                handleData(currentEvent, data);
            }
        }

        private void handleData(String event, String data) {
            if (event == null || data.equals("[DONE]")) return;
            try {
                JsonNode node = objectMapper.readTree(data);
                switch (event) {
                    case "content_block_start" -> handleContentBlockStart(node);
                    case "content_block_delta" -> handleContentBlockDelta(node);
                    case "content_block_stop" -> handleContentBlockStop();
                    case "message_stop" -> {
                        completed = true;
                        sink.onDone();
                    }
                    case "message_delta", "message_start", "ping" -> { /* no-op */ }
                    default -> log.debug("Unhandled SSE event: {}", event);
                }
            } catch (Exception e) {
                log.warn("Failed to parse SSE data for event '{}': {}", event, data, e);
            }
        }

        private void handleContentBlockStart(JsonNode node) {
            JsonNode block = node.path("content_block");
            String type = block.path("type").asText();
            if ("tool_use".equals(type)) {
                currentToolName = block.path("name").asText();
                toolInputAccumulator.setLength(0);
                inToolUseBlock = true;
                sink.onToolUse(currentToolName, Map.of());
            } else {
                inToolUseBlock = false;
            }
        }

        private void handleContentBlockDelta(JsonNode node) {
            JsonNode delta = node.path("delta");
            String type = delta.path("type").asText();
            if ("text_delta".equals(type)) {
                sink.onToken(delta.path("text").asText(""));
            } else if ("input_json_delta".equals(type) && inToolUseBlock) {
                toolInputAccumulator.append(delta.path("partial_json").asText(""));
            }
        }

        private void handleContentBlockStop() {
            if (inToolUseBlock && currentToolName != null) {
                try {
                    String json = toolInputAccumulator.toString().trim();
                    if (json.isEmpty()) json = "{}";
                    Map<String, Object> input = objectMapper.readValue(
                            json,
                            new TypeReference<>() {});
                    // Re-emit tool use with full parsed input so the service can execute it
                    sink.onToolUse(currentToolName, input);
                } catch (Exception e) {
                    log.warn("Failed to parse tool input JSON for tool '{}'", currentToolName, e);
                    sink.onToolUse(currentToolName, Map.of());
                }
                inToolUseBlock = false;
                currentToolName = null;
            }
        }

        boolean isCompleted() {
            return completed;
        }
    }
}
