package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.ProgressPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {
    Page<ProgressPhoto> findByUserIdOrderByDateDesc(UUID userId, Pageable pageable);
    List<ProgressPhoto> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate start, LocalDate end);
    Optional<ProgressPhoto> findByIdAndUserId(UUID id, UUID userId);
}
