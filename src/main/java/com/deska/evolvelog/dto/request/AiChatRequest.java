package com.deska.evolvelog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AiChatRequest(
        @NotNull(message = "conversationId is required")
        UUID conversationId,

        @NotBlank(message = "message is required")
        String message,

        String pageContext
) {}
