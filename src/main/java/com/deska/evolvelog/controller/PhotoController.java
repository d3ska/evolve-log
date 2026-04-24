package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.PhotoCategory;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.ProgressPhotoDto;
import com.deska.evolvelog.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /**
     * POST /api/photos  (multipart/form-data)
     * Fields: file, date (ISO), category (FRONT|BACK|SIDE|FULL_BODY), notes (optional)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgressPhotoDto>> upload(
            @AuthenticationPrincipal User user,
            @RequestParam MultipartFile file,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam PhotoCategory category,
            @RequestParam(required = false) String notes
    ) {
        ProgressPhotoDto dto = ProgressPhotoDto.from(
                photoService.upload(user, file, date, category, notes));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProgressPhotoDto>>> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProgressPhotoDto> result = photoService.findAll(user.getId(), page, size)
                .map(ProgressPhotoDto::from);
        return ResponseEntity.ok(ApiResponse.paged(result.getContent(),
                result.getTotalElements(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgressPhotoDto>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        ProgressPhotoDto dto = ProgressPhotoDto.from(photoService.findById(id, user.getId()));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * GET /api/photos/{id}/file — streams the actual image bytes
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        Resource resource = photoService.loadFile(id, user.getId());
        String contentType = resolveContentType(resource.getFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        photoService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private String resolveContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
