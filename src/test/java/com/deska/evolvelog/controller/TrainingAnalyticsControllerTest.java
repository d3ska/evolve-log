package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.ProgressiveOverloadDto;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.dto.response.WeeklyMuscleVolumeDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.GlobalExceptionHandler;
import com.deska.evolvelog.service.ProgressiveOverloadService;
import com.deska.evolvelog.service.TrainingVolumeService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrainingAnalyticsControllerTest {

    @Mock
    private TrainingVolumeService volumeService;

    @Mock
    private ProgressiveOverloadService overloadService;

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

        var controller = new TrainingAnalyticsController(volumeService, overloadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver())
                .build();
    }

    // ── GET /sessions/{id}/volume ─────────────────────────────────────────────

    @Test
    void shouldReturn200WithSessionVolumeSummary() throws Exception {
        // given
        UUID sessionId = UUID.randomUUID();
        var summary = new SessionVolumeSummaryDto(sessionId, new BigDecimal("4800"), null, 0.5, 2, List.of());
        when(volumeService.getSessionVolumeSummary(eq(sessionId), eq(userId))).thenReturn(summary);

        // when / then
        mockMvc.perform(get("/api/analytics/sessions/{id}/volume", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVolumeLoad").value(4800))
                .andExpect(jsonPath("$.data.exerciseCount").value(2));
    }

    @Test
    void shouldReturn404WhenSessionNotFound() throws Exception {
        // given
        UUID sessionId = UUID.randomUUID();
        when(volumeService.getSessionVolumeSummary(eq(sessionId), eq(userId)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        // when / then
        mockMvc.perform(get("/api/analytics/sessions/{id}/volume", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /volume/weekly ────────────────────────────────────────────────────

    @Test
    void shouldReturn200WithWeeklyVolume() throws Exception {
        // given
        var row = new WeeklyMuscleVolumeDto(LocalDate.of(2025, 1, 6), "chest", new BigDecimal("2400"), 2L, 2L);
        when(volumeService.getWeeklyVolumeByMuscle(eq(userId), any(), any(), isNull()))
                .thenReturn(List.of(row));

        // when / then
        mockMvc.perform(get("/api/analytics/volume/weekly")
                        .param("from", "2025-01-01")
                        .param("to", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].muscle").value("chest"))
                .andExpect(jsonPath("$.data[0].volumeLoad").value(2400));
    }

    @Test
    void shouldReturn400WhenDateRangeExceedsFiftyTwoWeeks() throws Exception {
        // given
        when(volumeService.getWeeklyVolumeByMuscle(eq(userId), any(), any(), isNull()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Date range must not exceed 52 weeks"));

        // when / then
        mockMvc.perform(get("/api/analytics/volume/weekly")
                        .param("from", "2024-01-01")
                        .param("to", "2025-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /progressive-overload/{id} ────────────────────────────────────────

    @Test
    void shouldReturn200WithProgressiveOverloadHistory() throws Exception {
        // given
        UUID defId = UUID.randomUUID();
        var dto = new ProgressiveOverloadDto(defId, "Barbell Bench Press", List.of());
        when(overloadService.getProgressiveOverload(eq(userId), eq(defId), eq(12))).thenReturn(dto);

        // when / then
        mockMvc.perform(get("/api/analytics/progressive-overload/{id}", defId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exerciseName").value("Barbell Bench Press"))
                .andExpect(jsonPath("$.data.history").isArray());
    }

    @Test
    void shouldReturn404WhenExerciseDefinitionNotFound() throws Exception {
        // given
        UUID defId = UUID.randomUUID();
        when(overloadService.getProgressiveOverload(eq(userId), eq(defId), anyInt()))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Exercise definition not found"));

        // when / then
        mockMvc.perform(get("/api/analytics/progressive-overload/{id}", defId))
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
