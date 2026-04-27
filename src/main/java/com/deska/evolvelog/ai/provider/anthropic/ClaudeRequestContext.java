package com.deska.evolvelog.ai.provider.anthropic;

/**
 * Thread-local holder for the Anthropic API key.
 * Services set the key before delegating to ClaudeAdapter and clear it in a finally block.
 */
public final class ClaudeRequestContext {

    private static final ThreadLocal<String> API_KEY = new ThreadLocal<>();

    private ClaudeRequestContext() {}

    public static void setApiKey(String apiKey) {
        API_KEY.set(apiKey);
    }

    public static String getApiKey() {
        return API_KEY.get();
    }

    public static void clear() {
        API_KEY.remove();
    }
}
