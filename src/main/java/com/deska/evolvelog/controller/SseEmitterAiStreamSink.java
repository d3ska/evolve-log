package com.deska.evolvelog.controller;

import com.deska.evolvelog.ai.provider.AiStreamSink;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * Adapts {@link AiStreamSink} callbacks to typed SSE events sent over an {@link SseEmitter}.
 *
 * <ul>
 *   <li>{@code event: token} — streaming text token</li>
 *   <li>{@code event: tool_use} — tool invocation started</li>
 *   <li>{@code event: tool_result} — tool execution completed</li>
 *   <li>{@code event: done} — stream finished successfully</li>
 *   <li>{@code event: error} — stream finished with an error</li>
 * </ul>
 */
public class SseEmitterAiStreamSink implements AiStreamSink {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterAiStreamSink.class);

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private volatile boolean done = false;

    public SseEmitterAiStreamSink(SseEmitter emitter, ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onToken(String text) {
        send("token", Map.of("t", text));
    }

    @Override
    public void onToolUse(String toolName, Map<String, Object> input) {
        send("tool_use", Map.of("tool", toolName));
    }

    @Override
    public void onToolResult(String toolName, int rowCount) {
        send("tool_result", Map.of("tool", toolName, "rowCount", rowCount));
    }

    @Override
    public void onDone() {
        if (done) return;
        done = true;
        try {
            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE emitter already closed on done", e);
        }
    }

    @Override
    public void onError(Throwable cause) {
        if (done) return;
        done = true;
        try {
            emitter.send(SseEmitter.event().name("error").data(cause.getMessage()));
            emitter.complete();
        } catch (IOException | IllegalStateException ignored) {
            log.debug("SSE emitter already closed on error");
        }
    }

    private void send(String eventName, Object data) {
        if (done) return;
        try {
            String json = data instanceof String s ? s : toJson(data);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE send failed for event '{}', emitter likely closed", eventName, e);
            done = true;
        }
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize SSE data to JSON", e);
            return "{}";
        }
    }
}
