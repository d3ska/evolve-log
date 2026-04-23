package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.AttachmentType;
import com.deska.evolvelog.domain.MediaAttachment;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.MediaAttachmentRepository;
import com.deska.evolvelog.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAttachmentService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024L;

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "video/mp4", "mp4",
            "video/quicktime", "mov",
            "video/webm", "webm"
    );

    private final MediaAttachmentRepository attachmentRepository;
    private final StorageService storageService;

    @Transactional
    public MediaAttachment upload(User user, MultipartFile file, LocalDate date, String notes) {
        AttachmentType mediaType = detectAndValidate(file);
        String filePath = storageService.store(file, user.getId());

        MediaAttachment attachment = MediaAttachment.builder()
                .user(user)
                .date(date)
                .filePath(filePath)
                .mediaType(mediaType)
                .originalFilename(file.getOriginalFilename())
                .notes(notes)
                .build();

        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public Page<MediaAttachment> findAll(UUID userId, int page, int size) {
        return attachmentRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<MediaAttachment> findByDateRange(UUID userId, LocalDate from, LocalDate to) {
        return attachmentRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
    }

    @Transactional(readOnly = true)
    public MediaAttachment findById(UUID id, UUID userId) {
        return attachmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaAttachment", id));
    }

    @Transactional(readOnly = true)
    public Resource loadFile(UUID id, UUID userId) {
        MediaAttachment attachment = findById(id, userId);
        return storageService.load(attachment.getFilePath());
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        MediaAttachment attachment = findById(id, userId);
        storageService.delete(attachment.getFilePath());
        attachmentRepository.delete(attachment);
    }

    public String resolveContentType(MediaAttachment attachment) {
        String filename = attachment.getFilePath();
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private AttachmentType detectAndValidate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not determine file type");
        }
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Image size must not exceed 10MB");
            }
            return AttachmentType.IMAGE;
        }
        if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
            if (file.getSize() > MAX_VIDEO_SIZE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Video size must not exceed 200MB");
            }
            return AttachmentType.VIDEO;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Unsupported file type. Allowed: JPEG, PNG, WebP, MP4, MOV, WebM");
    }
}
