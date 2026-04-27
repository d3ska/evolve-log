package com.deska.evolvelog.controller;

import com.deska.evolvelog.ai.router.AiTaskType;
import com.deska.evolvelog.domain.AiInsight;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.service.AiInsightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiInsightControllerTest {

    @Mock
    private AiInsightService aiInsightService;

    private MockMvc mockMvc;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var controller = new AiInsightController(aiInsightService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver())
                .build();
    }

    private HandlerMethodArgumentResolver principalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };
    }

    @Test
    void shouldReturnEmptyListWhenNoInsightsExist() throws Exception {
        // given
        when(aiInsightService.listInsights(eq(mockUser.getId()), isNull(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of()));

        // when / then
        mockMvc.perform(get("/api/ai/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturnInsightListWhenInsightsExist() throws Exception {
        // given
        AiInsight insight = AiInsight.builder()
                .id(UUID.randomUUID())
                .userId(mockUser.getId())
                .type("WEEKLY_REPORT")
                .periodStart(LocalDate.of(2025, 1, 13))
                .periodEnd(LocalDate.of(2025, 1, 20))
                .content("Your training this week was excellent.")
                .modelUsed("claude-sonnet-4-6")
                .generatedAt(OffsetDateTime.now())
                .build();
        when(aiInsightService.listInsights(eq(mockUser.getId()), isNull(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(insight)));

        // when / then
        mockMvc.perform(get("/api/ai/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("WEEKLY_REPORT"))
                .andExpect(jsonPath("$.data[0].content").value("Your training this week was excellent."));
    }

    @Test
    void shouldReturnGeneratedInsightWhenManualTrigger() throws Exception {
        // given
        AiInsight insight = AiInsight.builder()
                .id(UUID.randomUUID())
                .userId(mockUser.getId())
                .type("DAILY_SUMMARY")
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now())
                .content("Today you trained chest and triceps.")
                .modelUsed("claude-haiku-4-5-20251001")
                .generatedAt(OffsetDateTime.now())
                .build();
        when(aiInsightService.generateInsight(eq(mockUser.getId()), eq(AiTaskType.DAILY_SUMMARY)))
                .thenReturn(insight);

        // when / then
        mockMvc.perform(post("/api/ai/insights/generate")
                        .param("type", "DAILY_SUMMARY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("DAILY_SUMMARY"))
                .andExpect(jsonPath("$.data.content").value("Today you trained chest and triceps."));
    }
}
