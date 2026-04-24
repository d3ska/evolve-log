package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.SupplementPlanEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplementPlanEntryRepository extends JpaRepository<SupplementPlanEntry, UUID> {
    Optional<SupplementPlanEntry> findByIdAndPlanUserId(UUID id, UUID userId);
}
