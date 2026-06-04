package com.deska.evolvelog.ai.provider.gemini;

import com.deska.evolvelog.ai.provider.AiMessage;
import com.deska.evolvelog.ai.provider.AiProvider;
import com.deska.evolvelog.ai.provider.AiRequest;
import com.deska.evolvelog.ai.provider.AiResponse;
import com.deska.evolvelog.ai.provider.AiStreamSink;
import com.deska.evolvelog.ai.provider.AiToolDefinition;
import com.deska.evolvelog.ai.provider.ModelTier;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GeminiAdapter implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAdapter.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String providerId() {
        return "google";
    }

    @Override
    public void prepareContext(String apiKey) {
        GeminiRequestContext.setApiKey(apiKey);
    }

    @Override
    public void clearContext() {
        GeminiRequestContext.clear();
    }

    @Override
    public String modelIdForTier(ModelTier tier) {
        return switch (tier) {
            case FAST -> GeminiModelConstants.FLASH;
            case BALANCED, SMART -> GeminiModelConstants.PRO;
        };
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String apiKey = GeminiRequestContext.getApiKey();
        if (apiKey == null) {
            throw new IllegalStateException("No Gemini API key configured for this request");
        }

        try {
            String body = buildRequestBody(request);
            String url = BASE_URL + request.modelId() + ":generateContent?key=" + apiKey;
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Gemini API error: HTTP " + response.statusCode() + " — " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractText(root);
            int inputTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
            int outputTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);
            return new AiResponse(content, inputTokens, outputTokens);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call Gemini API", e);
        }
    }

    @Override
    public void stream(AiRequest request, AiStreamSink sink) {
        String apiKey = GeminiRequestContext.getApiKey();
        if (apiKey == null) {
            throw new IllegalStateException("No Gemini API key configured for this request");
        }

        try {
            String body = buildRequestBody(request);
            String url = BASE_URL + request.modelId() + ":streamGenerateContent?alt=sse&key=" + apiKey;
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = response.body().collect(Collectors.joining("\n"));
                log.error("Gemini API error: HTTP {} — {}", response.statusCode(), errorBody);
                sink.onError(new IllegalStateException("Gemini API error: HTTP " + response.statusCode()));
                return;
            }

            response.body().forEach(line -> parseSseLine(line, sink));
            sink.onDone();

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error streaming from Gemini API", e);
            sink.onError(e);
        }
    }

    void parseSseLine(String line, AiStreamSink sink) {
        if (!line.startsWith("data:")) return;
        String data = line.substring(5).trim();
        if (data.isEmpty() || data.equals("[DONE]")) return;

        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray()) return;

            for (JsonNode part : parts) {
                if (part.has("text")) {
                    sink.onToken(part.path("text").asText(""));
                } else if (part.has("functionCall")) {
                    JsonNode fc = part.path("functionCall");
                    String toolName = fc.path("name").asText();
                    Map<String, Object> args = objectMapper.convertValue(
                            fc.path("args"), new TypeReference<>() {});
                    sink.onToolUse(toolName, args != null ? args : Map.of());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini SSE line: {}", line, e);
        }
    }

    String buildRequestBody(AiRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();

            // System instruction
            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                body.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", request.systemPrompt()))
                ));
            }

            // Contents — remap "assistant" role to "model"
            List<Map<String, Object>> contents = new ArrayList<>();
            for (AiMessage msg : request.messages()) {
                String role = "assistant".equals(msg.role()) ? "model" : msg.role();
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.content()))
                ));
            }
            body.put("contents", contents);

            // Tools — functionDeclarations with uppercased JSON Schema types
            if (!request.tools().isEmpty()) {
                List<Map<String, Object>> functionDeclarations = request.tools().stream()
                        .map(this::toFunctionDeclaration)
                        .toList();
                body.put("tools", List.of(Map.of("functionDeclarations", functionDeclarations)));
            }

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Gemini request body", e);
        }
    }

    private Map<String, Object> toFunctionDeclaration(AiToolDefinition tool) {
        Map<String, Object> decl = new LinkedHashMap<>();
        decl.put("name", tool.name());
        decl.put("description", tool.description());
        decl.put("parameters", uppercaseTypes(tool.inputSchema()));
        return decl;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> uppercaseTypes(Map<String, Object> schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if ("type".equals(key) && value instanceof String s) {
                result.put(key, s.toUpperCase());
            } else if (value instanceof Map) {
                result.put(key, uppercaseTypes((Map<String, Object>) value));
            } else if (value instanceof List<?> list) {
                result.put(key, list.stream()
                        .map(item -> item instanceof Map ? uppercaseTypes((Map<String, Object>) item) : item)
                        .toList());
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private String extractText(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    return part.path("text").asText("");
                }
            }
        }
        return "";
    }
}
