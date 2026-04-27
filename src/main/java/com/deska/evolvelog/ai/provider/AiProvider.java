package com.deska.evolvelog.ai.provider;

public interface AiProvider {

    AiResponse complete(AiRequest request);

    void stream(AiRequest request, AiStreamSink sink);

    String providerId();
}
