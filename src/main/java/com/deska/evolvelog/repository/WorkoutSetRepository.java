package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {

    @Query("""
            SELECT ws FROM WorkoutSet ws
            WHERE ws.exercise.id = :exerciseId
              AND ws.setNumber = :setNumber
              AND ws.exercise.workoutSession.user.id = :userId
            """)
    Optional<WorkoutSet> findByExerciseIdAndSetNumberAndUserId(
            @Param("exerciseId") UUID exerciseId,
            @Param("setNumber") Integer setNumber,
            @Param("userId") UUID userId);
}
