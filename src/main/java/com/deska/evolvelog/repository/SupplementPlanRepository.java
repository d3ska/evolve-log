package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.SupplementPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplementPlanRepository extends JpaRepository<SupplementPlan, UUID> {

    @EntityGraph(attributePaths = {"entries", "entries.supplement"})
    List<SupplementPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"entries", "entries.supplement"})
    Optional<SupplementPlan> findByIdAndUserId(UUID id, UUID userId);
}
