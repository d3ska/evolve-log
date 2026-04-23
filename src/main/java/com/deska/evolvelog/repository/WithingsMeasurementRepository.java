package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.WithingsMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WithingsMeasurementRepository extends JpaRepository<WithingsMeasurement, UUID> {

    List<WithingsMeasurement> findByUserIdOrderByDateDesc(UUID userId);

    List<WithingsMeasurement> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDate from, LocalDate to);

    Optional<WithingsMeasurement> findByUserIdAndDate(UUID userId, LocalDate date);
}
