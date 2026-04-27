package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.MonthlyExerciseAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MonthlyAggregateRepository extends JpaRepository<MonthlyExerciseAggregate, UUID> {

    List<MonthlyExerciseAggregate> findByUserIdAndExerciseNameOrderByYearMonthDesc(UUID userId, String exerciseName);

    List<MonthlyExerciseAggregate> findByUserIdAndYearMonthOrderByExerciseNameAsc(UUID userId, LocalDate yearMonth);

    /**
     * Recomputes and UPSERTs all monthly exercise aggregates from the exercises table.
     * Intended for scheduled batch refresh.
     */
    @Modifying
    @Query(value = """
            INSERT INTO monthly_exercise_aggregates
                (id, user_id, exercise_name, exercise_definition_id, year_month,
                 max_weight_kg, total_volume_kg, session_count, total_sets, computed_at)
            SELECT gen_random_uuid(),
                   ws.user_id,
                   e.name,
                   MAX(e.exercise_definition_id),
                   DATE_TRUNC('month', ws.date)::date,
                   MAX(e.weight_kg),
                   SUM(e.sets * COALESCE(e.reps, 0) * COALESCE(e.weight_kg, 0)),
                   COUNT(DISTINCT ws.id),
                   SUM(e.sets),
                   NOW()
            FROM exercises e
            JOIN workout_sessions ws ON e.workout_session_id = ws.id
            WHERE e.weight_kg IS NOT NULL
            GROUP BY ws.user_id, e.name, DATE_TRUNC('month', ws.date)::date
            ON CONFLICT (user_id, exercise_name, year_month) DO UPDATE SET
                max_weight_kg          = EXCLUDED.max_weight_kg,
                total_volume_kg        = EXCLUDED.total_volume_kg,
                session_count          = EXCLUDED.session_count,
                total_sets             = EXCLUDED.total_sets,
                exercise_definition_id = EXCLUDED.exercise_definition_id,
                computed_at            = NOW()
            """, nativeQuery = true)
    void upsertAllFromExerciseSets();

    /**
     * Natively UPSERTs a monthly aggregate row.
     * ON CONFLICT updates all aggregate fields but preserves id and created timestamps.
     */
    @Modifying
    @Query(value = """
            INSERT INTO monthly_exercise_aggregates
                (id, user_id, exercise_name, exercise_definition_id, year_month,
                 max_weight_kg, total_volume_kg, session_count, total_sets, computed_at)
            VALUES
                (gen_random_uuid(), :userId, :exerciseName, :exerciseDefinitionId, :yearMonth,
                 :maxWeightKg, :totalVolumeKg, :sessionCount, :totalSets, NOW())
            ON CONFLICT (user_id, exercise_name, year_month) DO UPDATE SET
                max_weight_kg          = EXCLUDED.max_weight_kg,
                total_volume_kg        = EXCLUDED.total_volume_kg,
                session_count          = EXCLUDED.session_count,
                total_sets             = EXCLUDED.total_sets,
                exercise_definition_id = EXCLUDED.exercise_definition_id,
                computed_at            = NOW()
            """, nativeQuery = true)
    void upsert(
            @Param("userId") UUID userId,
            @Param("exerciseName") String exerciseName,
            @Param("exerciseDefinitionId") UUID exerciseDefinitionId,
            @Param("yearMonth") LocalDate yearMonth,
            @Param("maxWeightKg") BigDecimal maxWeightKg,
            @Param("totalVolumeKg") BigDecimal totalVolumeKg,
            @Param("sessionCount") int sessionCount,
            @Param("totalSets") int totalSets
    );
}
