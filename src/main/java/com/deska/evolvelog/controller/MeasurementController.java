package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.CreateMeasurementRequest;
import com.deska.evolvelog.dto.request.UpdateMeasurementRequest;
import com.deska.evolvelog.dto.response.MeasurementDto;
import com.deska.evolvelog.service.MeasurementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeasurementDto>> create(
            @Valid @RequestBody CreateMeasurementRequest request,
            @AuthenticationPrincipal User user) {

        Measurement measurement = measurementService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MeasurementDto.from(measurement)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<MeasurementDto>>> findAll(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Measurement> measurements = measurementService.findAll(user.getId(), page, size);
        java.util.List<MeasurementDto> dtos = measurements.getContent().stream()
                .map(MeasurementDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.paged(dtos, measurements.getTotalElements(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MeasurementDto>> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Measurement measurement = measurementService.findById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(MeasurementDto.from(measurement)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MeasurementDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMeasurementRequest request,
            @AuthenticationPrincipal User user) {

        Measurement measurement = measurementService.update(id, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(MeasurementDto.from(measurement)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        measurementService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
