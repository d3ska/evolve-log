package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.Measurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {
    Page<Measurement> findByUserIdOrderByDateDesc(UUID userId, Pageable pageable);
    List<Measurement> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate start, LocalDate end);
    Optional<Measurement> findByIdAndUserId(UUID id, UUID userId);
    Optional<Measurement> findFirstByUserIdOrderByDateDesc(UUID userId);
}
