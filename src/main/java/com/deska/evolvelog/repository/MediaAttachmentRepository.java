package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.MediaAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAttachmentRepository extends JpaRepository<MediaAttachment, UUID> {
    Page<MediaAttachment> findByUserIdOrderByDateDesc(UUID userId, Pageable pageable);
    List<MediaAttachment> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate from, LocalDate to);
    Optional<MediaAttachment> findByIdAndUserId(UUID id, UUID userId);
}
