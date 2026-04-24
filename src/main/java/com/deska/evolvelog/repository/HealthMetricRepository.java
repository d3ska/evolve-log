package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.HealthMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface HealthMetricRepository extends JpaRepository<HealthMetric, UUID> {

    List<HealthMetric> findByUserIdAndSourceOrderByDateDesc(UUID userId, String source);

    List<HealthMetric> findByUserIdAndSourceAndDateBetweenOrderByDateAsc(
            UUID userId, String source, LocalDate from, LocalDate to);

    List<HealthMetric> findByUserIdAndDateBetweenOrderByDateAsc(
            UUID userId, LocalDate from, LocalDate to);

    @Modifying
    @Query(value = """
            INSERT INTO health_metrics (id, user_id, source, date, metric_key, value, unit, recorded_at)
            VALUES (gen_random_uuid(), :userId, :source, :date, :metricKey, :value, :unit, :recordedAt)
            ON CONFLICT (user_id, source, date, metric_key)
            DO UPDATE SET value = EXCLUDED.value, recorded_at = EXCLUDED.recorded_at
            """, nativeQuery = true)
    void upsert(
            @Param("userId") UUID userId,
            @Param("source") String source,
            @Param("date") LocalDate date,
            @Param("metricKey") String metricKey,
            @Param("value") BigDecimal value,
            @Param("unit") String unit,
            @Param("recordedAt") OffsetDateTime recordedAt);
}
