package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.*;
import com.deska.evolvelog.dto.response.*;
import com.deska.evolvelog.service.SupplementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/supplements")
public class SupplementController {

    private final SupplementService supplementService;

    public SupplementController(SupplementService supplementService) {
        this.supplementService = supplementService;
    }

    // ── Supplement catalog ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<SupplementDto>> createSupplement(
            @Valid @RequestBody CreateSupplementRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(supplementService.createSupplement(user, req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplementDto>>> listSupplements(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(supplementService.listSupplements(user.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplement(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        supplementService.deleteSupplement(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Plans ───────────────────────────────────────────────────────────────

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<SupplementPlanDto>> createPlan(
            @Valid @RequestBody CreateSupplementPlanRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(supplementService.createPlan(user, req)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SupplementPlanDto>>> listPlans(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(supplementService.listPlans(user.getId())));
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<SupplementPlanDto>> getPlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(supplementService.getPlan(planId, user.getId())));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User user) {
        supplementService.deletePlan(planId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Plan entries ────────────────────────────────────────────────────────

    @PostMapping("/plans/{planId}/entries")
    public ResponseEntity<ApiResponse<SupplementPlanDto>> addEntry(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateSupplementPlanEntryRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(supplementService.addEntry(planId, user.getId(), req)));
    }

    @DeleteMapping("/plans/{planId}/entries/{entryId}")
    public ResponseEntity<ApiResponse<Void>> removeEntry(
            @PathVariable UUID planId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal User user) {
        supplementService.removeEntry(planId, entryId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Intake log ──────────────────────────────────────────────────────────

    @PostMapping("/logs")
    public ResponseEntity<ApiResponse<SupplementLogDto>> logIntake(
            @Valid @RequestBody LogSupplementRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(supplementService.logIntake(user, req)));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<SupplementLogDto>>> listLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "50") int limit) {
        List<SupplementLogDto> logs = date != null
                ? supplementService.listLogsByDate(user.getId(), date)
                : supplementService.listLogs(user.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @DeleteMapping("/logs/{logId}")
    public ResponseEntity<ApiResponse<Void>> deleteLog(
            @PathVariable UUID logId,
            @AuthenticationPrincipal User user) {
        supplementService.deleteLog(logId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
