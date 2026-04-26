package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.SupplementSource;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.SupplementDto;
import com.deska.evolvelog.dto.response.SupplementLogDto;
import com.deska.evolvelog.service.SupplementCatalogService;
import com.deska.evolvelog.service.SupplementLogService;
import com.deska.evolvelog.service.SupplementPlanService;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupplementControllerTest {

    @Mock
    SupplementCatalogService catalogService;

    @Mock
    SupplementPlanService planService;

    @Mock
    SupplementLogService logService;

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

        var controller = new SupplementController(catalogService, planService, logService);
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
    void shouldReturn201WhenCreatingSupplement() throws Exception {
        // given
        var dto = new SupplementDto(UUID.randomUUID(), "Vitamin D", null, null, null, OffsetDateTime.now());
        when(catalogService.createSupplement(any(), any())).thenReturn(dto);

        // when / then
        mockMvc.perform(post("/api/supplements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vitamin D"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Vitamin D"));
    }

    @Test
    void shouldReturn200WithListWhenListingSupplements() throws Exception {
        // given
        var dto = new SupplementDto(UUID.randomUUID(), "Creatine", null, null, null, OffsetDateTime.now());
        when(catalogService.listSupplements(any())).thenReturn(List.of(dto));

        // when / then
        mockMvc.perform(get("/api/supplements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Creatine"));
    }

    @Test
    void shouldReturn204WhenDeletingSupplement() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        doNothing().when(catalogService).deleteSupplement(any(), any());

        // when / then
        mockMvc.perform(delete("/api/supplements/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn201WhenLoggingIntake() throws Exception {
        // given
        UUID supplementId = UUID.randomUUID();
        var dto = new SupplementLogDto(
                UUID.randomUUID(), null, null, OffsetDateTime.now(),
                null, null, SupplementSource.SPONTANEOUS, null);
        when(logService.logIntake(any(), any())).thenReturn(dto);

        // when / then
        mockMvc.perform(post("/api/supplements/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplementId":"%s"}
                                """.formatted(supplementId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturn200WithLogsWhenListingByDate() throws Exception {
        // given
        when(logService.listLogsByDate(any(), any())).thenReturn(List.of());

        // when / then
        mockMvc.perform(get("/api/supplements/logs").param("date", "2025-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldReturn204WhenDeletingLog() throws Exception {
        // given
        UUID logId = UUID.randomUUID();
        doNothing().when(logService).deleteLog(any(), any());

        // when / then
        mockMvc.perform(delete("/api/supplements/logs/{logId}", logId))
                .andExpect(status().isNoContent());
    }
}
