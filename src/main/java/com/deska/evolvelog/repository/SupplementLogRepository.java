package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.SupplementLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplementLogRepository extends JpaRepository<SupplementLog, UUID> {
    List<SupplementLog> findByUserIdOrderByTakenAtDesc(UUID userId, Pageable pageable);
    Optional<SupplementLog> findByIdAndUserId(UUID id, UUID userId);
}
