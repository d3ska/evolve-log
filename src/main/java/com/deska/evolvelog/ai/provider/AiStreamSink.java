package com.deska.evolvelog.ai.provider;

import java.util.Map;

public interface AiStreamSink {

    void onToken(String text);

    void onToolUse(String toolName, Map<String, Object> input);

    void onToolResult(String toolName, int rowCount);

    void onDone();

    void onError(Throwable cause);
}
