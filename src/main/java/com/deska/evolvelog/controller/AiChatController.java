package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.AiChatRequest;
import com.deska.evolvelog.service.AiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    private final AiChatService aiChatService;
    private final ChatRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public AiChatController(AiChatService aiChatService, ChatRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.aiChatService = aiChatService;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PostMapping
    public SseEmitter chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal User user) {

        UUID userId = user.getId();
        if (!rateLimiter.tryAcquire(userId)) {
            return sseError("Rate limit exceeded. Max 1 concurrent stream and 30 requests per hour.");
        }

        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onCompletion(() -> rateLimiter.release(userId));
        emitter.onTimeout(() -> rateLimiter.release(userId));
        emitter.onError(e -> rateLimiter.release(userId));

        SseEmitterAiStreamSink sink = new SseEmitterAiStreamSink(emitter, objectMapper);

        executor.submit(() -> aiChatService.chat(
                userId,
                request.conversationId(),
                request.message(),
                request.pageContext(),
                sink));

        return emitter;
    }

    private static SseEmitter sseError(String message) {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
        } catch (IOException ignored) {
            emitter.completeWithError(ignored);
        }
        return emitter;
    }
}
