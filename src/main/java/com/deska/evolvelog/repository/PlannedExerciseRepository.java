package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.PlannedExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlannedExerciseRepository extends JpaRepository<PlannedExercise, UUID> {

    @Query("SELECT e FROM PlannedExercise e WHERE e.id = :id AND e.trainingPlan.user.id = :userId")
    Optional<PlannedExercise> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    int countByTrainingPlanId(UUID trainingPlanId);

    void deleteAllByTrainingPlanId(UUID trainingPlanId);
}
