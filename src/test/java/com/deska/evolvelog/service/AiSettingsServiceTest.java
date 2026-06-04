package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.AiSettings;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.AiSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSettingsServiceTest {

    @Mock
    private AiSettingsRepository aiSettingsRepository;

    private AiSettingsService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AiSettingsService(aiSettingsRepository);
        userId = UUID.randomUUID();
        lenient().when(aiSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(aiSettingsRepository.findById(userId)).thenReturn(Optional.empty());
    }

    // --- 9.8: Provider validation ---

    @Test
    void shouldRejectAnthropicKeyWithoutRequiredPrefix() {
        // given
        String invalidKey = "some-random-key-without-prefix";

        // when / then
        assertThatThrownBy(() -> service.saveSettings(userId, "anthropic", invalidKey))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("sk-ant-");
    }

    @Test
    void shouldAcceptAnthropicKeyWithCorrectPrefix() {
        // given
        String validKey = "sk-ant-api03-validkey";

        // when
        AiSettings result = service.saveSettings(userId, "anthropic", validKey);

        // then
        verify(aiSettingsRepository).save(any());
        assertThat(result.getProvider()).isEqualTo("anthropic");
    }

    @Test
    void shouldAcceptGoogleKeyWithAnyNonBlankValue() {
        // given
        String googleKey = "AIzaSyAny-valid-google-api-key";

        // when
        AiSettings result = service.saveSettings(userId, "google", googleKey);

        // then
        verify(aiSettingsRepository).save(any());
        assertThat(result.getProvider()).isEqualTo("google");
    }

    @Test
    void shouldAcceptGoogleKeyThatDoesNotStartWithSkAnt() {
        // given — a key that would be rejected for anthropic but accepted for google
        String key = "non-prefixed-key-12345";

        // when / then — no exception
        service.saveSettings(userId, "google", key);
        verify(aiSettingsRepository).save(any());
    }

    @Test
    void shouldReturnDefaultAnthropicProviderWhenNoSettingsExist() {
        // given
        when(aiSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        String provider = service.getProvider(userId);

        // then
        assertThat(provider).isEqualTo("anthropic");
    }

    @Test
    void shouldReturnStoredProviderWhenSettingsExist() {
        // given
        AiSettings settings = AiSettings.builder()
                .userId(userId)
                .provider("google")
                .apiKeyEncrypted("AIzaSyAny-key")
                .build();
        when(aiSettingsRepository.findById(userId)).thenReturn(Optional.of(settings));

        // when
        String provider = service.getProvider(userId);

        // then
        assertThat(provider).isEqualTo("google");
    }
}
