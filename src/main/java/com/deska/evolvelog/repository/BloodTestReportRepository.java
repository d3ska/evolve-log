package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.BloodTestReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodTestReportRepository extends JpaRepository<BloodTestReport, UUID> {

    @EntityGraph(attributePaths = {"results"})
    List<BloodTestReport> findByUserIdOrderByDateDesc(UUID userId);

    @EntityGraph(attributePaths = {"results"})
    Optional<BloodTestReport> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    @Query("SELECT r.id FROM BloodTestReport r WHERE r.user.id = :userId AND r.date = :date AND r.labName = :labName")
    UUID findIdByUserIdAndDateAndLabName(@Param("userId") UUID userId,
                                        @Param("date") LocalDate date,
                                        @Param("labName") String labName);
}
