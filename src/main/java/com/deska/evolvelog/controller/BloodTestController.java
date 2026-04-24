package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.BloodTestHistoryPointDto;
import com.deska.evolvelog.dto.response.BloodTestReportDetailDto;
import com.deska.evolvelog.dto.response.BloodTestReportSummaryDto;
import com.deska.evolvelog.service.BloodTestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blood-tests")
public class BloodTestController {

    private final BloodTestService bloodTestService;

    public BloodTestController(BloodTestService bloodTestService) {
        this.bloodTestService = bloodTestService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BloodTestReportSummaryDto>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        BloodTestReportSummaryDto dto = bloodTestService.upload(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BloodTestReportSummaryDto>>> findAll(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.success(bloodTestService.findAll(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodTestReportDetailDto>> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.success(bloodTestService.findById(id, user.getId())));
    }

    @GetMapping("/history/{key}")
    public ResponseEntity<ApiResponse<List<BloodTestHistoryPointDto>>> history(
            @PathVariable String key,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.success(bloodTestService.findHistory(user.getId(), key)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        bloodTestService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
