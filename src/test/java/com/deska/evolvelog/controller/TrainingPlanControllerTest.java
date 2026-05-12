package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.GlobalExceptionHandler;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.service.TrainingPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingPlanControllerTest {

    @Mock
    TrainingPlanService planService;

    private MockMvc mockMvc;
    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var controller = new TrainingPlanController(planService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver())
                .build();
    }

    // ── POST /{planId}/sync-from-session/{sessionId} ──────────────────────────

    @Test
    void syncFromSession_returns200_onSuccess() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        TrainingPlan plan = TrainingPlan.builder()
                .id(planId).user(mockUser).name("Push Day").isActive(true)
                .plannedExercises(List.of()).build();

        when(planService.syncFromSession(eq(planId), eq(sessionId), eq(userId))).thenReturn(plan);

        mockMvc.perform(post("/api/training-plans/{planId}/sync-from-session/{sessionId}", planId, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(planId.toString()))
                .andExpect(jsonPath("$.data.name").value("Push Day"));
    }

    @Test
    void syncFromSession_returns409_whenSessionIsActive() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(planService.syncFromSession(eq(planId), eq(sessionId), eq(userId)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT,
                        "Cannot sync from an active session — finish the session first"));

        mockMvc.perform(post("/api/training-plans/{planId}/sync-from-session/{sessionId}", planId, sessionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void syncFromSession_returns404_whenSessionBelongsToOtherUser() throws Exception {
        UUID planId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(planService.syncFromSession(eq(planId), eq(sessionId), eq(userId)))
                .thenThrow(new ResourceNotFoundException("WorkoutSession", sessionId));

        mockMvc.perform(post("/api/training-plans/{planId}/sync-from-session/{sessionId}", planId, sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
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
}
