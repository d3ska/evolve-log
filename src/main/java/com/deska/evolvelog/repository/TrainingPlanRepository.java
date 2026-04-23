package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.TrainingPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

    List<TrainingPlan> findByUserIdOrderByCreatedAtAsc(UUID userId);

    @EntityGraph(attributePaths = {"plannedExercises"})
    @Query("SELECT p FROM TrainingPlan p WHERE p.id = :id AND p.user.id = :userId")
    Optional<TrainingPlan> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
