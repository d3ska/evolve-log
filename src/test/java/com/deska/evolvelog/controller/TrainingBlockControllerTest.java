package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.TrainingBlockDto;
import com.deska.evolvelog.exception.GlobalExceptionHandler;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.service.TrainingBlockService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrainingBlockControllerTest {

    @Mock
    TrainingBlockService blockService;

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

        var controller = new TrainingBlockController(blockService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
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

    private TrainingBlockDto sampleDto() {
        return new TrainingBlockDto(UUID.randomUUID(), "Block A", "Hypertrophy", true, OffsetDateTime.now());
    }

    @Test
    void post_validRequest_returns201() throws Exception {
        when(blockService.create(any(), any())).thenReturn(sampleDto());

        mockMvc.perform(post("/api/training-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Block A","description":"Hypertrophy"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Block A"));
    }

    @Test
    void post_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/training-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":"desc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void get_returns200WithList() throws Exception {
        when(blockService.list(any())).thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/training-blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Block A"));
    }

    @Test
    void patch_validId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(blockService.update(any(), eq(id), any())).thenReturn(sampleDto());

        mockMvc.perform(patch("/api/training-blocks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void patch_unknownId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(blockService.update(any(), eq(unknownId), any()))
                .thenThrow(new ResourceNotFoundException("TrainingBlock", unknownId));

        mockMvc.perform(patch("/api/training-blocks/{id}", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void delete_validId_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(blockService).delete(any(), eq(id), eq(false));

        mockMvc.perform(delete("/api/training-blocks/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_withDeletePlansTrue_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(blockService).delete(any(), eq(id), eq(true));

        mockMvc.perform(delete("/api/training-blocks/{id}", id)
                        .param("deletePlans", "true"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("TrainingBlock", unknownId))
                .when(blockService).delete(any(), eq(unknownId), eq(false));

        mockMvc.perform(delete("/api/training-blocks/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
