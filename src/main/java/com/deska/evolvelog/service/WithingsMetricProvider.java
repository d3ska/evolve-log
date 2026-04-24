package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WithingsToken;
import com.deska.evolvelog.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WithingsMetricProvider implements HealthMetricProvider {

    private static final Logger log = LoggerFactory.getLogger(WithingsMetricProvider.class);

    private static final String SOURCE      = "withings";
    private static final String MEASURE_URL = "https://wbsapi.withings.net/measure";

    private record MetricDef(String key, String unit) {}

    private static final Map<Integer, MetricDef> METRICS = Map.ofEntries(
            Map.entry(1,   new MetricDef("weight_kg",           "kg")),
            Map.entry(5,   new MetricDef("fat_free_mass_kg",    "kg")),
            Map.entry(6,   new MetricDef("body_fat_percent",    "%")),
            Map.entry(8,   new MetricDef("fat_mass_weight_kg",  "kg")),
            Map.entry(11,  new MetricDef("heart_pulse_bpm",     "bpm")),
            Map.entry(76,  new MetricDef("muscle_mass_kg",      "kg")),
            Map.entry(77,  new MetricDef("hydration_kg",        "kg")),
            Map.entry(88,  new MetricDef("bone_mass_kg",        "kg")),
            Map.entry(91,  new MetricDef("pulse_wave_velocity", "m/s")),
            Map.entry(123, new MetricDef("vo2_max",             "ml/min/kg")),
            Map.entry(155, new MetricDef("vascular_age",        "years")),
            Map.entry(167, new MetricDef("nerve_health_score",  "score")),
            Map.entry(170, new MetricDef("visceral_fat",        "level")),
            Map.entry(226, new MetricDef("basal_metabolic_rate","kcal")),
            Map.entry(227, new MetricDef("metabolic_age",       "years"))
    );

    private final WithingsService withingsService;
    private final HealthMetricService healthMetricService;
    private final RestClient restClient;

    public WithingsMetricProvider(WithingsService withingsService,
                                  HealthMetricService healthMetricService) {
        this.withingsService = withingsService;
        this.healthMetricService = healthMetricService;
        this.restClient = RestClient.create();
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    @Transactional
    public int sync(User user) {
        WithingsToken token = withingsService.getValidToken(user);

        JsonNode response = restClient.post()
                .uri(MEASURE_URL)
                .header("Authorization", "Bearer " + token.getAccessToken())
                .body(buildParams())
                .retrieve()
                .body(JsonNode.class);

        validateStatus(response);
        return parseAndStore(user, response.get("body"));
    }

    private MultiValueMap<String, String> buildParams() {
        String meastypes = METRICS.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("action", "getmeas");
        params.add("meastypes", meastypes);
        params.add("category", "1");
        return params;
    }

    private int parseAndStore(User user, JsonNode body) {
        int count = 0;
        for (JsonNode group : body.get("measuregrps")) {
            long epochSeconds = group.get("date").asLong();
            LocalDate date = Instant.ofEpochSecond(epochSeconds)
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDate();

            for (JsonNode measure : group.get("measures")) {
                int type = measure.get("type").asInt();
                MetricDef def = METRICS.get(type);
                if (def == null) {
                    log.debug("Unknown Withings measure type {}, skipping", type);
                    continue;
                }

                BigDecimal value = BigDecimal.valueOf(measure.get("value").asLong())
                        .scaleByPowerOfTen(measure.get("unit").asInt())
                        .setScale(4, RoundingMode.HALF_UP);

                healthMetricService.upsert(user, SOURCE, date, def.key(), value, def.unit());
                count++;
            }
        }
        return count;
    }

    private void validateStatus(JsonNode response) {
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
