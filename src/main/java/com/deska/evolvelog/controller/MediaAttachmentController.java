package com.deska.evolvelog.controller;

import com.deska.evolvelog.domain.MediaAttachment;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.ApiResponse;
import com.deska.evolvelog.dto.response.MediaAttachmentDto;
import com.deska.evolvelog.service.MediaAttachmentService;
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
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaAttachmentController {

    private final MediaAttachmentService mediaAttachmentService;

    /**
     * POST /api/media  (multipart/form-data)
     * Fields: file, date (ISO), notes (optional)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaAttachmentDto>> upload(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam MultipartFile file,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String notes
    ) {
        MediaAttachmentDto dto = MediaAttachmentDto.from(
                mediaAttachmentService.upload(user, file, date, notes));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaAttachmentDto>>> getAll(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MediaAttachmentDto> result = mediaAttachmentService.findAll(user.getId(), page, size)
                .map(MediaAttachmentDto::from);
        return ResponseEntity.ok(ApiResponse.paged(result.getContent(),
                result.getTotalElements(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaAttachmentDto>> getById(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        MediaAttachmentDto dto = MediaAttachmentDto.from(
                mediaAttachmentService.findById(id, user.getId()));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * GET /api/media/{id}/file — streams the actual file bytes (image or video)
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        MediaAttachment attachment = mediaAttachmentService.findById(id, user.getId());
        Resource resource = mediaAttachmentService.loadFile(id, user.getId());
        String contentType = mediaAttachmentService.resolveContentType(attachment);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        mediaAttachmentService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
