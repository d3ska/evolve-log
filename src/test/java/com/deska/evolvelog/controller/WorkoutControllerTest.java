package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.DeviationEntryDto;
import com.deska.evolvelog.dto.response.FinishedSessionDto;
import com.deska.evolvelog.dto.response.SessionDeviationDto;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.dto.response.WorkoutSessionDto;
import com.deska.evolvelog.exception.GlobalExceptionHandler;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.service.WorkoutDeviationService;
import com.deska.evolvelog.service.WorkoutService;
import com.deska.evolvelog.service.WorkoutSessionFlowService;
import com.deska.evolvelog.service.WorkoutSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkoutControllerTest {

    @Mock
    private WorkoutService workoutService;

    @Mock
    private WorkoutSessionFlowService flowService;

    @Mock
    private WorkoutSetService workoutSetService;

    @Mock
    private WorkoutDeviationService deviationService;

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

        var controller = new WorkoutController(workoutService, flowService, workoutSetService, deviationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver())
                .build();
    }

    // ── POST /sessions/start ──────────────────────────────────────────────────

    @Test
    void shouldReturn201WhenStartFromValidPlan() throws Exception {
        // given
        UUID planId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId)
                .user(mockUser)
                .date(now)
                .startedAt(now)
                .build();

        when(flowService.startFromPlan(any(User.class), eq(planId))).thenReturn(session);

        // when / then
        mockMvc.perform(post("/api/workouts/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingPlanId\":\"" + planId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sessionId.toString()));
    }

    @Test
    void shouldReturn404WhenStartFromUnknownPlan() throws Exception {
        // given
        UUID planId = UUID.randomUUID();
        when(flowService.startFromPlan(any(User.class), eq(planId)))
                .thenThrow(new ResourceNotFoundException("TrainingPlan", planId));

        // when / then
        mockMvc.perform(post("/api/workouts/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainingPlanId\":\"" + planId + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /sessions/{id}/finish ────────────────────────────────────────────

    @Test
    void shouldReturn200WhenFinishActiveSession() throws Exception {
        // given
        UUID sessionId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(45);
        LocalDateTime finishedAt = LocalDateTime.now();

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId)
                .user(mockUser)
                .date(startedAt)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .durationMinutes(45)
                .build();
        WorkoutSessionDto sessionDto = WorkoutSessionDto.from(session);
        SessionVolumeSummaryDto volume = new SessionVolumeSummaryDto(
                sessionId, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0, List.of());
        FinishedSessionDto finished = new FinishedSessionDto(sessionDto, volume);

        when(flowService.finishSession(eq(sessionId), eq(userId))).thenReturn(finished);

        // when / then
        mockMvc.perform(post("/api/workouts/sessions/{id}/finish", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.session.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.volume.totalVolumeLoad").value(0));
    }

    @Test
    void shouldReturn200WhenFinishAlreadyFinishedSession() throws Exception {
        // given — idempotent: same response returned
        UUID sessionId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusHours(1);
        LocalDateTime finishedAt = LocalDateTime.now().minusMinutes(10);

        WorkoutSession session = WorkoutSession.builder()
                .id(sessionId)
                .user(mockUser)
                .date(startedAt)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .durationMinutes(50)
                .build();
        WorkoutSessionDto sessionDto = WorkoutSessionDto.from(session);
        SessionVolumeSummaryDto volume = new SessionVolumeSummaryDto(
                sessionId, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0, List.of());
        FinishedSessionDto finished = new FinishedSessionDto(sessionDto, volume);

        when(flowService.finishSession(eq(sessionId), eq(userId))).thenReturn(finished);

        // when / then
        mockMvc.perform(post("/api/workouts/sessions/{id}/finish", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /sessions/{id}/deviations ─────────────────────────────────────────

    @Test
    void shouldReturn200WithDeviationEntries() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID peId = UUID.randomUUID();
        DeviationEntryDto entry = new DeviationEntryDto("SKIPPED", peId, "Squat", 4, 5, 8, null, null);
        SessionDeviationDto dto = new SessionDeviationDto(sessionId, true, List.of(entry));

        when(deviationService.getDeviations(eq(sessionId), eq(userId))).thenReturn(dto);

        mockMvc.perform(get("/api/workouts/sessions/{id}/deviations", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.supported").value(true))
                .andExpect(jsonPath("$.data.entries[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.data.entries[0].name").value("Squat"));
    }

    @Test
    void shouldReturn200WithUnsupportedWhenNoSnapshot() throws Exception {
        UUID sessionId = UUID.randomUUID();
        SessionDeviationDto dto = new SessionDeviationDto(sessionId, false, List.of());

        when(deviationService.getDeviations(eq(sessionId), eq(userId))).thenReturn(dto);

        mockMvc.perform(get("/api/workouts/sessions/{id}/deviations", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supported").value(false))
                .andExpect(jsonPath("$.data.entries").isEmpty());
    }

    @Test
    void shouldReturn404WhenDeviationsRequestedForOtherUsersSession() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(deviationService.getDeviations(eq(sessionId), eq(userId)))
                .thenThrow(new ResourceNotFoundException("WorkoutSession", sessionId));

        mockMvc.perform(get("/api/workouts/sessions/{id}/deviations", sessionId))
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
