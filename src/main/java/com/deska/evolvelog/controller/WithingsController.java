package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.domain.WithingsToken;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.WithingsExchangeRequest;
import com.deska.evolvelog.dto.response.WithingsMeasurementDto;
import com.deska.evolvelog.dto.response.WithingsStatusDto;
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

    public WithingsController(WithingsService withingsService) {
        this.withingsService = withingsService;
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

        int count = withingsService.syncMeasurements(user);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/api/withings/measurements")
    public ResponseEntity<ApiResponse<List<WithingsMeasurementDto>>> measurements(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<WithingsMeasurementDto> data = (from != null && to != null)
                ? withingsService.getMeasurementsInRange(user.getId(), from, to).stream()
                        .map(WithingsMeasurementDto::from).toList()
                : withingsService.getMeasurements(user.getId()).stream()
                        .map(WithingsMeasurementDto::from).toList();

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/api/withings/connection")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @AuthenticationPrincipal User user) {

        withingsService.disconnect(user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
