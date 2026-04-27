package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.ai.provider.AiToolDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

/**
 * Contract for AI tools callable during streaming conversations.
 * Each tool is a Spring bean collected by {@link AiToolRegistry}.
 */
public interface AiTool {

    /** Unique tool name as declared to the AI model. */
    String name();

    /** Human-readable description used in the tool definition sent to the model. */
    String description();

    /** JSON Schema describing the tool's input parameters. */
    java.util.Map<String, Object> inputSchema();

    /**
     * Execute the tool with the given input and return a plain-text result.
     *
     * @param input  parsed JSON input from the model
     * @param userId the authenticated user's ID — all queries must be scoped to this user
     */
    String execute(ObjectNode input, UUID userId);

    /** Convenience: build an {@link AiToolDefinition} from this tool. */
    default AiToolDefinition toDefinition() {
        return new AiToolDefinition(name(), description(), inputSchema());
    }
}
