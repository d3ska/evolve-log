package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WithingsToken;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.WithingsTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WithingsService {

    private static final Logger log = LoggerFactory.getLogger(WithingsService.class);

    private static final String TOKEN_URL = "https://wbsapi.withings.net/v2/oauth2";

    private final WithingsTokenRepository tokenRepository;
    private final RestClient restClient;

    @Value("${withings.client-id}")
    private String clientId;

    @Value("${withings.client-secret}")
    private String clientSecret;

    @Value("${withings.redirect-uri}")
    private String redirectUri;

    public WithingsService(WithingsTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
        this.restClient = RestClient.create();
    }

    // ─── OAuth ────────────────────────────────────────────────────────────────

    public String buildAuthUrl() {
        String params = "response_type=code" +
                "&client_id=" + clientId +
                "&scope=user.metrics" +
                "&redirect_uri=" + redirectUri;
        return "https://account.withings.com/oauth2_user/authorize2?" + params;
    }

    @Transactional
    public void exchangeCode(User user, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("action", "requesttoken");
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        JsonNode response = restClient.post()
                .uri(TOKEN_URL)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        validateWithingsStatus(response);
        saveTokens(user, response.get("body"));
    }

    @Transactional
    public void disconnect(User user) {
        tokenRepository.deleteByUserId(user.getId());
    }

    // ─── Token access ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WithingsToken findToken(UUID userId) {
        return tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Withings account not connected"));
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return tokenRepository.findByUserId(userId).isPresent();
    }

    /**
     * Returns a valid (non-expired) token for the user, refreshing it first if necessary.
     * Called by {@link WithingsMetricProvider} before each API request.
     */
    @Transactional
    public WithingsToken getValidToken(User user) {
        WithingsToken token = findToken(user.getId());
        if (token.isExpired()) {
            refreshToken(user, token);
            token = findToken(user.getId());
        }
        return token;
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    private void refreshToken(User user, WithingsToken token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("action", "requesttoken");
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", token.getRefreshToken());

        JsonNode response = restClient.post()
                .uri(TOKEN_URL)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        validateWithingsStatus(response);
        saveTokens(user, response.get("body"));
    }

    private void saveTokens(User user, JsonNode body) {
        String accessToken  = body.get("access_token").asText();
        String refreshToken = body.get("refresh_token").asText();
        long expiresIn      = body.get("expires_in").asLong();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);

        WithingsToken token = tokenRepository.findByUserId(user.getId())
                .orElse(WithingsToken.builder().user(user).build());

        token.updateTokens(accessToken, refreshToken, expiresAt);
        tokenRepository.save(token);
    }

    private void validateWithingsStatus(JsonNode response) {
        if (response == null || !response.has("status")) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Invalid response from Withings");
        }
        int status = response.get("status").asInt();
        if (status != 0) {
            String error = response.has("error") ? response.get("error").asText() : "Unknown error";
            log.error("Withings API error: status={}, error={}", status, error);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Withings error: " + error);
        }
    }
}
