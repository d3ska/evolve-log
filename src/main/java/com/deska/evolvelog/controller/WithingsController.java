package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WithingsToken;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.WithingsExchangeRequest;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.dto.response.WithingsStatusDto;
import com.deska.evolvelog.service.HealthMetricService;
import com.deska.evolvelog.service.WithingsMetricProvider;
import com.deska.evolvelog.service.WithingsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class WithingsController {

    private final WithingsService withingsService;
    private final WithingsMetricProvider withingsMetricProvider;
    private final HealthMetricService healthMetricService;

    public WithingsController(WithingsService withingsService,
                              WithingsMetricProvider withingsMetricProvider,
                              HealthMetricService healthMetricService) {
        this.withingsService = withingsService;
        this.withingsMetricProvider = withingsMetricProvider;
        this.healthMetricService = healthMetricService;
    }

    @GetMapping("/api/withings/auth-url")
    public ResponseEntity<ApiResponse<String>> authUrl() {
        return ResponseEntity.ok(ApiResponse.success(withingsService.buildAuthUrl()));
    }

    @PostMapping("/api/withings/exchange")
    public ResponseEntity<ApiResponse<WithingsStatusDto>> exchange(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WithingsExchangeRequest request) {

        withingsService.exchangeCode(user, request.code());
        WithingsToken token = withingsService.findToken(user.getId());
        return ResponseEntity.ok(ApiResponse.success(WithingsStatusDto.from(token)));
    }

    @GetMapping("/api/withings/status")
    public ResponseEntity<ApiResponse<WithingsStatusDto>> status(
            @AuthenticationPrincipal User user) {

        if (!withingsService.isConnected(user.getId())) {
            return ResponseEntity.ok(ApiResponse.success(WithingsStatusDto.disconnected()));
        }
        WithingsToken token = withingsService.findToken(user.getId());
        return ResponseEntity.ok(ApiResponse.success(WithingsStatusDto.from(token)));
    }

    @PostMapping("/api/withings/sync")
    public ResponseEntity<ApiResponse<Integer>> sync(
            @AuthenticationPrincipal User user) {

        int count = withingsMetricProvider.sync(user);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/api/withings/measurements")
    public ResponseEntity<ApiResponse<List<DailyHealthMetricsDto>>> measurements(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DailyHealthMetricsDto> data = healthMetricService
                .getDailyMetrics(user.getId(), withingsMetricProvider.source(), from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/api/withings/connection")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal User user) {

        withingsService.disconnect(user);
        return ResponseEntity.noContent().build();
    }
}
