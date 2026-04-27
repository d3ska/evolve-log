package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.AiSettings;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.service.AiSettingsService;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiSettingsControllerTest {

    @Mock
    private AiSettingsService aiSettingsService;

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

        var controller = new AiSettingsController(aiSettingsService);
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
    void shouldReturnHasApiKeyFalseWhenNoSettingsExist() throws Exception {
        // given
        when(aiSettingsService.getSettings(mockUser.getId())).thenReturn(Optional.empty());

        // when / then
        mockMvc.perform(get("/api/ai/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasApiKey").value(false))
                .andExpect(jsonPath("$.data.apiKeyHint").doesNotExist());
    }

    @Test
    void shouldReturnMaskedHintWhenApiKeyExists() throws Exception {
        // given
        AiSettings settings = AiSettings.builder()
                .userId(mockUser.getId())
                .provider("anthropic")
                .apiKeyEncrypted("sk-ant-abc1234567890xyz")
                .build();
        when(aiSettingsService.getSettings(mockUser.getId())).thenReturn(Optional.of(settings));

        // when / then
        mockMvc.perform(get("/api/ai/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasApiKey").value(true))
                .andExpect(jsonPath("$.data.provider").value("anthropic"))
                .andExpect(jsonPath("$.data.apiKeyHint").value("sk-a...0xyz"));
    }

    @Test
    void shouldSaveSettingsAndReturnMaskedViewWhenRequestValid() throws Exception {
        // given
        AiSettings saved = AiSettings.builder()
                .userId(mockUser.getId())
                .provider("anthropic")
                .apiKeyEncrypted("sk-ant-newkey12345678")
                .build();
        when(aiSettingsService.saveSettings(eq(mockUser.getId()), eq("anthropic"), eq("sk-ant-newkey12345678")))
                .thenReturn(saved);

        // when / then
        mockMvc.perform(put("/api/ai/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"anthropic","apiKey":"sk-ant-newkey12345678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasApiKey").value(true))
                .andExpect(jsonPath("$.data.provider").value("anthropic"));
    }

    @Test
    void shouldReturn400WhenApiKeyIsBlank() throws Exception {
        // when / then
        mockMvc.perform(put("/api/ai/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"anthropic","apiKey":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenProviderIsBlank() throws Exception {
        // when / then
        mockMvc.perform(put("/api/ai/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"","apiKey":"sk-ant-somekey"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
