package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.request.*;
import com.deska.evolvelog.dto.response.*;
import com.deska.evolvelog.dto.response.SupplementTodayDto;
import com.deska.evolvelog.service.SupplementCatalogService;
import com.deska.evolvelog.service.SupplementLogService;
import com.deska.evolvelog.service.SupplementPlanService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/supplements")
public class SupplementController {

    private final SupplementCatalogService catalogService;
    private final SupplementPlanService planService;
    private final SupplementLogService logService;

    public SupplementController(SupplementCatalogService catalogService,
                                SupplementPlanService planService,
                                SupplementLogService logService) {
        this.catalogService = catalogService;
        this.planService = planService;
        this.logService = logService;
    }

    // ── Supplement catalog ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<SupplementDto>> createSupplement(
            @Valid @RequestBody CreateSupplementRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(catalogService.createSupplement(user, req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplementDto>>> listSupplements(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.listSupplements(user.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplement(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        catalogService.deleteSupplement(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Plans ───────────────────────────────────────────────────────────────

    @GetMapping("/plans/today")
    public ResponseEntity<ApiResponse<List<SupplementTodayDto>>> getTodayStatus(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(logService.getTodayStatus(user.getId())));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<SupplementPlanDto>> createPlan(
            @Valid @RequestBody CreateSupplementPlanRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planService.createPlan(user, req)));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SupplementPlanDto>>> listPlans(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(planService.listPlans(user.getId())));
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<SupplementPlanDto>> getPlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlan(planId, user.getId())));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User user) {
        planService.deletePlan(planId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Plan entries ────────────────────────────────────────────────────────

    @PostMapping("/plans/{planId}/entries")
    public ResponseEntity<ApiResponse<SupplementPlanDto>> addEntry(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateSupplementPlanEntryRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(planService.addEntry(planId, user.getId(), req)));
    }

    @DeleteMapping("/plans/{planId}/entries/{entryId}")
    public ResponseEntity<Void> removeEntry(
            @PathVariable UUID planId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal User user) {
        planService.removeEntry(planId, entryId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Intake log ──────────────────────────────────────────────────────────

    @PostMapping("/logs")
    public ResponseEntity<ApiResponse<SupplementLogDto>> logIntake(
            @Valid @RequestBody LogSupplementRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(logService.logIntake(user, req)));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<SupplementLogDto>>> listLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "50") int limit) {
        List<SupplementLogDto> logs = date != null
                ? logService.listLogsByDate(user.getId(), date)
                : logService.listLogs(user.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @DeleteMapping("/logs/{logId}")
    public ResponseEntity<Void> deleteLog(
            @PathVariable UUID logId,
            @AuthenticationPrincipal User user) {
        logService.deleteLog(logId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
