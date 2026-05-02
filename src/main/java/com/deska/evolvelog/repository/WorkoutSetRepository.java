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
            WHERE ws.id = :setId
              AND ws.exercise.workoutSession.user.id = :userId
            """)
    Optional<WorkoutSet> findByIdAndUserId(@Param("setId") UUID setId, @Param("userId") UUID userId);
}
