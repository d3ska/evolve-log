package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.SupplementLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Query(nativeQuery = true, value = """
            SELECT
                sp.id::varchar              AS planId,
                sp.name                     AS planName,
                spe.id::varchar             AS entryId,
                spe.supplement_id::varchar  AS supplementId,
                s.name                      AS supplementName,
                spe.time_slot               AS timeSlot,
                spe.dose_amount             AS doseAmount,
                spe.dose_unit               AS doseUnit,
                spe.sort_order              AS sortOrder,
                (sl.id IS NOT NULL)         AS takenToday,
                sl.taken_at                 AS loggedAt,
                sl.id::varchar              AS logId
            FROM supplement_plans sp
            LEFT JOIN supplement_plan_entries spe ON spe.plan_id = sp.id
            LEFT JOIN supplements s               ON s.id = spe.supplement_id
            LEFT JOIN supplement_logs sl          ON sl.plan_entry_id = spe.id
                                                  AND sl.user_id = :userId
                                                  AND DATE(sl.taken_at) = CURRENT_DATE
            WHERE sp.user_id = :userId
              AND sp.active  = true
            ORDER BY sp.name, spe.sort_order
            """)
    List<TodayStatusRow> findTodayStatus(@Param("userId") UUID userId);

    interface TodayStatusRow {
        String getPlanId();
        String getPlanName();
        String getEntryId();
        String getSupplementId();
        String getSupplementName();
        String getTimeSlot();
        BigDecimal getDoseAmount();
        String getDoseUnit();
        int getSortOrder();
        boolean getTakenToday();
        Instant getLoggedAt();
        String getLogId();
    }
}
