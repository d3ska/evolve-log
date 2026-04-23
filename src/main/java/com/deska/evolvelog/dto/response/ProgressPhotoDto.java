package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.PhotoCategory;
import com.deska.evolvelog.domain.ProgressPhoto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProgressPhotoDto(
        UUID id,
        LocalDate date,
        PhotoCategory category,
        String notes,
        String fileUrl,
        LocalDateTime createdAt
) {
    public static ProgressPhotoDto from(ProgressPhoto photo) {
        return new ProgressPhotoDto(
                photo.getId(),
                photo.getDate(),
                photo.getCategory(),
                photo.getNotes(),
                "/api/photos/" + photo.getId() + "/file",
                photo.getCreatedAt()
        );
    }
}
