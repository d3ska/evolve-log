package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.dto.response.ExerciseProgressPointDto;
import com.deska.evolvelog.dto.response.PersonalRecordDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    Optional<Exercise> findByIdAndWorkoutSessionUserId(UUID id, UUID userId);
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
}
