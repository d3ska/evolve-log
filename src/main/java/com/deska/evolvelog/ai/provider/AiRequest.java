package com.deska.evolvelog.ai.provider;

import java.util.List;

public record AiRequest(
        String modelId,
        String systemPrompt,
        List<AiMessage> messages,
        List<AiToolDefinition> tools,
        double temperature
) {
    public AiRequest {
        if (messages == null) messages = List.of();
        if (tools == null) tools = List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String modelId;
        private String systemPrompt = "";
        private List<AiMessage> messages = List.of();
        private List<AiToolDefinition> tools = List.of();
        private double temperature = 0.7;

        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder messages(List<AiMessage> messages) { this.messages = messages; return this; }
        public Builder tools(List<AiToolDefinition> tools) { this.tools = tools; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }

        public AiRequest build() {
            return new AiRequest(modelId, systemPrompt, messages, tools, temperature);
        }
    }
}
