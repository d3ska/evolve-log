package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.ai.provider.AiToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Collects all {@link AiTool} beans and provides lookup and definition-building utilities.
 */
@Component
public class AiToolRegistry {

    private final Map<String, AiTool> toolsByName;

    public AiToolRegistry(List<AiTool> tools) {
        this.toolsByName = tools.stream()
                .collect(Collectors.toUnmodifiableMap(AiTool::name, Function.identity()));
    }

    public Optional<AiTool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public List<AiToolDefinition> toDefinitions() {
        return toolsByName.values().stream()
                .map(AiTool::toDefinition)
                .toList();
    }
}
