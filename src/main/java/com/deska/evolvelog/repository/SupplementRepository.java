package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.Supplement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplementRepository extends JpaRepository<Supplement, UUID> {
    List<Supplement> findByUserIdOrderByNameAsc(UUID userId);
    Optional<Supplement> findByIdAndUserId(UUID id, UUID userId);
}
