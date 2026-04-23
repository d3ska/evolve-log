package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.AttachmentType;
import com.deska.evolvelog.domain.MediaAttachment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MediaAttachmentDto(
        UUID id,
        LocalDate date,
        AttachmentType mediaType,
        String originalFilename,
        String notes,
        String fileUrl,
        LocalDateTime createdAt
) {
    public static MediaAttachmentDto from(MediaAttachment attachment) {
        return new MediaAttachmentDto(
                attachment.getId(),
                attachment.getDate(),
                attachment.getMediaType(),
                attachment.getOriginalFilename(),
                attachment.getNotes(),
                "/api/media/" + attachment.getId() + "/file",
                attachment.getCreatedAt()
        );
    }
}
