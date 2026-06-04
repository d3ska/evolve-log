package com.deska.evolvelog.ai.provider.gemini;

final class GeminiRequestContext {

    private static final ThreadLocal<String> API_KEY = new ThreadLocal<>();

    private GeminiRequestContext() {
    }

    static void setApiKey(String apiKey) {
        API_KEY.set(apiKey);
    }

    static String getApiKey() {
        return API_KEY.get();
    }

    static void clear() {
        API_KEY.remove();
    }
}
