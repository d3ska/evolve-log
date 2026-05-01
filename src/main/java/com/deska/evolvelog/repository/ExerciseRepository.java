package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.dto.response.ExerciseProgressPointDto;
import com.deska.evolvelog.dto.response.PersonalRecordDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    Optional<Exercise> findByIdAndWorkoutSessionUserId(UUID id, UUID userId);

    @Query("""
            SELECT e FROM Exercise e
            LEFT JOIN FETCH e.workoutSets
            WHERE e.id = :id AND e.workoutSession.user.id = :userId
            """)
    Optional<Exercise> findByIdAndUserIdWithSets(@Param("id") UUID id, @Param("userId") UUID userId);

    int countByWorkoutSessionId(UUID workoutSessionId);

    // Personal records: heaviest set ever per exercise name for a user
    @Query("""
            SELECT new com.deska.evolvelog.dto.response.PersonalRecordDto(
                e.name,
                e.weightKg,
                e.sets,
                e.reps,
                e.workoutSession.date
            )
            FROM Exercise e
            WHERE e.workoutSession.user.id = :userId
              AND e.weightKg IS NOT NULL
              AND e.weightKg = (
                  SELECT MAX(e2.weightKg)
                  FROM Exercise e2
                  WHERE e2.name = e.name
                    AND e2.workoutSession.user.id = :userId
              )
            GROUP BY e.name, e.weightKg, e.sets, e.reps, e.workoutSession.date
            ORDER BY e.name ASC
            """)
    List<PersonalRecordDto> findPersonalRecordsByUserId(@Param("userId") UUID userId);

    // Personal records within a date range (for bi-weekly report)
    @Query("""
            SELECT new com.deska.evolvelog.dto.response.PersonalRecordDto(
                e.name,
                e.weightKg,
                e.sets,
                e.reps,
                e.workoutSession.date
            )
            FROM Exercise e
            WHERE e.workoutSession.user.id = :userId
              AND e.workoutSession.date BETWEEN :from AND :to
              AND e.weightKg IS NOT NULL
              AND e.weightKg = (
                  SELECT MAX(e2.weightKg)
                  FROM Exercise e2
                  WHERE e2.name = e.name
                    AND e2.workoutSession.user.id = :userId
                    AND e2.workoutSession.date BETWEEN :from AND :to
              )
            GROUP BY e.name, e.weightKg, e.sets, e.reps, e.workoutSession.date
            ORDER BY e.name ASC
            """)
    List<PersonalRecordDto> findPersonalRecordsByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // Exercise progression: all logged sets for a given exercise name, ordered by date (global)
    @Query("""
            SELECT new com.deska.evolvelog.dto.response.ExerciseProgressPointDto(
                e.workoutSession.date,
                e.weightKg,
                e.sets,
                e.reps,
                e.workoutSession.id
            )
            FROM Exercise e
            WHERE e.workoutSession.user.id = :userId
              AND LOWER(e.name) = LOWER(:exerciseName)
            ORDER BY e.workoutSession.date ASC
            """)
    List<ExerciseProgressPointDto> findProgressByUserIdAndExerciseName(
            @Param("userId") UUID userId,
            @Param("exerciseName") String exerciseName
    );

    // Exercise progression scoped to a specific training plan
    @Query("""
            SELECT new com.deska.evolvelog.dto.response.ExerciseProgressPointDto(
                e.workoutSession.date,
                e.weightKg,
                e.sets,
                e.reps,
                e.workoutSession.id
            )
            FROM Exercise e
            WHERE e.workoutSession.user.id = :userId
              AND LOWER(e.name) = LOWER(:exerciseName)
              AND e.workoutSession.trainingPlan.id = :planId
            ORDER BY e.workoutSession.date ASC
            """)
    List<ExerciseProgressPointDto> findProgressByUserIdAndExerciseNameAndPlanId(
            @Param("userId") UUID userId,
            @Param("exerciseName") String exerciseName,
            @Param("planId") UUID planId
    );

    // All distinct exercise names logged by a user (for autocomplete / listing)
    @Query("""
            SELECT DISTINCT e.name
            FROM Exercise e
            WHERE e.workoutSession.user.id = :userId
            ORDER BY e.name ASC
            """)
    List<String> findDistinctExerciseNamesByUserId(@Param("userId") UUID userId);

    // Volume analytics: all exercises for a session (owner-checked via join)
    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.workoutSession ws
            WHERE ws.id = :sessionId
              AND ws.user.id = :userId
            """)
    List<Exercise> findBySessionIdAndUserId(@Param("sessionId") UUID sessionId,
                                            @Param("userId") UUID userId);

    // Weekly volume aggregation by muscle group — native query for DATE_TRUNC
    // Volume is computed from workout_sets when exercise-level reps/weight are null (plan-based workouts),
    // falling back to exercise-level fields for older exercises logged without individual sets.
    @Query(value = """
            SELECT DATE_TRUNC('week', ws.date)::date AS week_start,
                   e.primary_muscle                  AS muscle,
                   SUM(
                       COALESCE(
                           (SELECT SUM(s.reps * s.weight_kg)
                            FROM workout_sets s
                            WHERE s.exercise_id = e.id
                              AND s.reps IS NOT NULL AND s.weight_kg IS NOT NULL),
                           CASE
                               WHEN e.sets IS NOT NULL AND e.reps IS NOT NULL AND e.weight_kg IS NOT NULL
                               THEN (e.sets * e.reps * e.weight_kg)
                           END
                       )
                   )                                 AS volume_load,
                   COUNT(DISTINCT ws.id)             AS session_count,
                   SUM(e.sets)                       AS set_count
            FROM exercises e
            JOIN workout_sessions ws ON e.workout_session_id = ws.id
            WHERE ws.user_id       = :userId
              AND e.primary_muscle IS NOT NULL
              AND ws.date BETWEEN :from AND :to
              AND (:muscle IS NULL OR e.primary_muscle = :muscle)
              AND (
                  EXISTS (
                      SELECT 1 FROM workout_sets s
                      WHERE s.exercise_id = e.id
                        AND s.reps IS NOT NULL AND s.weight_kg IS NOT NULL
                  )
                  OR (e.reps IS NOT NULL AND e.weight_kg IS NOT NULL AND e.sets IS NOT NULL)
              )
            GROUP BY week_start, e.primary_muscle
            ORDER BY week_start ASC, e.primary_muscle ASC
            """, nativeQuery = true)
    List<WeeklyVolumeRow> findWeeklyVolumeByMuscle(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("muscle") String muscle);

    // Progressive overload: last N sessions for a (userId, exerciseDefinitionId) pair
    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.workoutSession ws
            WHERE ws.user.id = :userId
              AND e.exerciseDefinitionId = :definitionId
            ORDER BY ws.date DESC
            """)
    List<Exercise> findByUserIdAndDefinitionId(
            @Param("userId") UUID userId,
            @Param("definitionId") UUID definitionId,
            Pageable pageable);

    // Progressive overload: date range for a (userId, exerciseDefinitionId) pair
    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.workoutSession ws
            WHERE ws.user.id = :userId
              AND e.exerciseDefinitionId = :definitionId
              AND CAST(ws.date AS LocalDate) BETWEEN :from AND :to
            ORDER BY ws.date ASC
            """)
    List<Exercise> findByUserIdAndDefinitionIdBetween(
            @Param("userId") UUID userId,
            @Param("definitionId") UUID definitionId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // Auto-link: exercises with no definition linked for a user
    @Query("""
            SELECT e FROM Exercise e
            JOIN e.workoutSession ws
            WHERE ws.user.id = :userId
              AND e.exerciseDefinitionId IS NULL
            """)
    List<Exercise> findUnlinkedByUserId(@Param("userId") UUID userId);

    // Auto-link: bulk update exercise_definition_id and primary_muscle by name match
    @Modifying
    @Query(value = """
            UPDATE exercises e
            SET exercise_definition_id = d.id,
                primary_muscle         = d.primary_muscle
            FROM exercise_definitions d
            WHERE LOWER(e.name) = LOWER(d.name)
              AND d.is_system = true
              AND e.exercise_definition_id IS NULL
              AND e.workout_session_id IN (
                  SELECT id FROM workout_sessions WHERE user_id = :userId
              )
            """, nativeQuery = true)
    int bulkAutoLinkByUserId(@Param("userId") UUID userId);
}
