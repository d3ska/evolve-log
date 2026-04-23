package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WithingsMeasurement;
import com.deska.evolvelog.domain.WithingsToken;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.WithingsMeasurementRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WithingsService {

    private static final Logger log = LoggerFactory.getLogger(WithingsService.class);

    private static final String TOKEN_URL   = "https://wbsapi.withings.net/v2/oauth2";
    private static final String MEASURE_URL = "https://wbsapi.withings.net/measure";

    // Withings measure types
    private static final int TYPE_WEIGHT      = 1;
    private static final int TYPE_BODY_FAT    = 6;
    private static final int TYPE_MUSCLE_MASS = 76;
    private static final int TYPE_BONE_MASS   = 88;

    private final WithingsTokenRepository tokenRepository;
    private final WithingsMeasurementRepository measurementRepository;
    private final RestClient restClient;

    @Value("${withings.client-id}")
    private String clientId;

    @Value("${withings.client-secret}")
    private String clientSecret;

    @Value("${withings.redirect-uri}")
    private String redirectUri;

    public WithingsService(
            WithingsTokenRepository tokenRepository,
            WithingsMeasurementRepository measurementRepository) {
        this.tokenRepository = tokenRepository;
        this.measurementRepository = measurementRepository;
        this.restClient = RestClient.create();
    }

    // ─── OAuth ────────────────────────────────────────────────────────────────

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

    // ─── Status ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WithingsToken findToken(UUID userId) {
        return tokenRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Withings account not connected"));
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return tokenRepository.findByUserId(userId).isPresent();
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────

    @Transactional
    public int syncMeasurements(User user) {
        WithingsToken token = getOrRefreshToken(user);

        JsonNode response = restClient.post()
                .uri(MEASURE_URL)
                .header("Authorization", "Bearer " + token.getAccessToken())
                .body(buildMeasureParams())
                .retrieve()
                .body(JsonNode.class);

        validateWithingsStatus(response);

        List<WithingsMeasurement> toSave = parseMeasurements(user, response.get("body"));
        int count = 0;
        for (WithingsMeasurement m : toSave) {
            measurementRepository.findByUserIdAndDate(user.getId(), m.getDate())
                    .ifPresentOrElse(
                            existing -> updateExisting(existing, m),
                            () -> measurementRepository.save(m)
                    );
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<WithingsMeasurement> getMeasurements(UUID userId) {
        return measurementRepository.findByUserIdOrderByDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<WithingsMeasurement> getMeasurementsInRange(UUID userId, LocalDate from, LocalDate to) {
        return measurementRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
    }

    // ─── Token refresh ────────────────────────────────────────────────────────

    private WithingsToken getOrRefreshToken(User user) {
        WithingsToken token = findToken(user.getId());
        if (token.isExpired()) {
            refreshToken(user, token);
        }
        return token;
    }

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

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void saveTokens(User user, JsonNode body) {
        String accessToken  = body.get("access_token").asText();
        String refreshToken = body.get("refresh_token").asText();
        long expiresIn      = body.get("expires_in").asLong();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);

        WithingsToken token = tokenRepository.findByUserId(user.getId())
                .orElse(WithingsToken.builder().user(user).build());

        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);
    }

    private MultiValueMap<String, String> buildMeasureParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("action", "getmeas");
        params.add("meastypes", TYPE_WEIGHT + "," + TYPE_BODY_FAT + "," + TYPE_MUSCLE_MASS + "," + TYPE_BONE_MASS);
        params.add("category", "1"); // real measurements only
        return params;
    }

    private List<WithingsMeasurement> parseMeasurements(User user, JsonNode body) {
        // Group measures by date (Withings groups by measuregrp, each with a date)
        Map<LocalDate, Map<Integer, BigDecimal>> byDate = new HashMap<>();

        for (JsonNode group : body.get("measuregrps")) {
            long epochSeconds = group.get("date").asLong();
            LocalDate date = Instant.ofEpochSecond(epochSeconds)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate();

            byDate.putIfAbsent(date, new HashMap<>());
            Map<Integer, BigDecimal> dayValues = byDate.get(date);

            for (JsonNode measure : group.get("measures")) {
                int type  = measure.get("type").asInt();
                long value = measure.get("value").asLong();
                int unit  = measure.get("unit").asInt();
                BigDecimal actual = BigDecimal.valueOf(value)
                        .scaleByPowerOfTen(unit)
                        .setScale(2, RoundingMode.HALF_UP);
                // Keep the latest value per type per day
                dayValues.merge(type, actual, (a, b) -> b);
            }
        }

        List<WithingsMeasurement> results = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<LocalDate, Map<Integer, BigDecimal>> entry : byDate.entrySet()) {
            Map<Integer, BigDecimal> vals = entry.getValue();
            results.add(WithingsMeasurement.builder()
                    .user(user)
                    .date(entry.getKey())
                    .weightKg(vals.get(TYPE_WEIGHT))
                    .bodyFatPercent(rescale(vals.get(TYPE_BODY_FAT), 1))
                    .muscleMassKg(vals.get(TYPE_MUSCLE_MASS))
                    .boneMassKg(vals.get(TYPE_BONE_MASS))
                    .recordedAt(now)
                    .build());
        }
        return results;
    }

    private void updateExisting(WithingsMeasurement existing, WithingsMeasurement updated) {
        if (updated.getWeightKg() != null)       existing.setWeightKg(updated.getWeightKg());
        if (updated.getBodyFatPercent() != null)  existing.setBodyFatPercent(updated.getBodyFatPercent());
        if (updated.getMuscleMassKg() != null)    existing.setMuscleMassKg(updated.getMuscleMassKg());
        if (updated.getBoneMassKg() != null)      existing.setBoneMassKg(updated.getBoneMassKg());
        measurementRepository.save(existing);
    }

    private BigDecimal rescale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
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
