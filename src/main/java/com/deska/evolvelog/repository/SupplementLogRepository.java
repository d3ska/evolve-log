package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.SupplementLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplementLogRepository extends JpaRepository<SupplementLog, UUID> {

    @EntityGraph(attributePaths = {"supplement", "planEntry"})
    List<SupplementLog> findByUserIdOrderByTakenAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"supplement", "planEntry"})
    List<SupplementLog> findByUserIdAndTakenAtBetweenOrderByTakenAtDesc(UUID userId, OffsetDateTime start, OffsetDateTime end);

    Optional<SupplementLog> findByIdAndUserId(UUID id, UUID userId);
}
