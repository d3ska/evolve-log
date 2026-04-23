package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.PhotoCategory;
import com.deska.evolvelog.domain.ProgressPhoto;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ProgressPhotoRepository;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    private final ProgressPhotoRepository photoRepository;
    private final StorageService storageService;

    @Transactional
    public ProgressPhoto upload(User user, MultipartFile file, LocalDate date,
                                PhotoCategory category, String notes) {
        validateImage(file);
        String filePath = storageService.store(file, user.getId());

        ProgressPhoto photo = ProgressPhoto.builder()
                .user(user)
                .date(date)
                .filePath(filePath)
                .category(category)
                .notes(notes)
                .build();

        return photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public Page<ProgressPhoto> findAll(UUID userId, int page, int size) {
        return photoRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public ProgressPhoto findById(UUID id, UUID userId) {
        return photoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ProgressPhoto", id));
    }

    @Transactional(readOnly = true)
    public Resource loadFile(UUID id, UUID userId) {
        ProgressPhoto photo = findById(id, userId);
        return storageService.load(photo.getFilePath());
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        ProgressPhoto photo = findById(id, userId);
        storageService.delete(photo.getFilePath());
        photoRepository.delete(photo);
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image size must not exceed 10MB");
        }
    }
}
