package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.BloodTestReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodTestReportRepository extends JpaRepository<BloodTestReport, UUID> {

    @EntityGraph(attributePaths = {"results"})
    List<BloodTestReport> findByUserIdOrderByDateDesc(UUID userId);

    @EntityGraph(attributePaths = {"results"})
    Optional<BloodTestReport> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
