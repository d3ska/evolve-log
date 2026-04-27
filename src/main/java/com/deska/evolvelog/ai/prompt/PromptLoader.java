package com.deska.evolvelog.ai.prompt;

import com.deska.evolvelog.ai.router.AiTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Component
public class PromptLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);

    private static final Map<AiTaskType, String> PROMPT_FILES = Map.of(
            AiTaskType.DAILY_SUMMARY, "prompts/DAILY_SUMMARY.txt",
            AiTaskType.WEEKLY_REPORT,  "prompts/WEEKLY_REPORT.txt",
            AiTaskType.BLOOD_ANALYSIS, "prompts/BLOOD_ANALYSIS.txt",
            AiTaskType.POST_WORKOUT,   "prompts/DAILY_SUMMARY.txt"
    );

    private final Map<AiTaskType, String> prompts;

    public PromptLoader() {
        this.prompts = loadAll();
        log.info("Loaded {} AI prompt templates", this.prompts.size());
    }

    public String getPrompt(AiTaskType taskType) {
        String prompt = prompts.get(taskType);
        if (prompt == null) {
            throw new IllegalArgumentException("No prompt template for task type: " + taskType);
        }
        return prompt;
    }

    public String getChatPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/CHAT.txt");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing required prompt file: prompts/CHAT.txt", e);
        }
    }

    private Map<AiTaskType, String> loadAll() {
        Map<AiTaskType, String> result = new EnumMap<>(AiTaskType.class);
        for (Map.Entry<AiTaskType, String> entry : PROMPT_FILES.entrySet()) {
            result.put(entry.getKey(), readClasspathResource(entry.getValue()));
        }
        return result;
    }

    private String readClasspathResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing required prompt file: " + path, e);
        }
    }
}
