package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.UnitSystem;
import com.deska.evolvelog.service.UserService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// T19 — PUT /api/user/me/locale updates preference and returns updated user

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .unitSystem(UnitSystem.METRIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        var controller = new UserController(userService);
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
    void shouldUpdateLocaleToPlAndReturnUpdatedUser() throws Exception {
        // given
        User updatedUser = User.builder()
                .id(mockUser.getId())
                .email(mockUser.getEmail())
                .name(mockUser.getName())
                .unitSystem(UnitSystem.METRIC)
                .locale("pl")
                .createdAt(mockUser.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateLocale(any(User.class), eq("pl"))).thenReturn(updatedUser);

        // when / then
        mockMvc.perform(put("/api/user/me/locale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale":"pl"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locale").value("pl"));
    }

    @Test
    void shouldUpdateLocaleToEnAndReturnUpdatedUser() throws Exception {
        // given
        User updatedUser = User.builder()
                .id(mockUser.getId())
                .email(mockUser.getEmail())
                .name(mockUser.getName())
                .unitSystem(UnitSystem.METRIC)
                .locale("en")
                .createdAt(mockUser.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateLocale(any(User.class), eq("en"))).thenReturn(updatedUser);

        // when / then
        mockMvc.perform(put("/api/user/me/locale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale":"en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locale").value("en"));
    }

    @Test
    void shouldReturn400WhenLocaleIsUnsupported() throws Exception {
        // when / then
        mockMvc.perform(put("/api/user/me/locale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale":"fr"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLocaleIsBlank() throws Exception {
        // when / then
        mockMvc.perform(put("/api/user/me/locale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLocaleIsMissing() throws Exception {
        // when / then
        mockMvc.perform(put("/api/user/me/locale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
