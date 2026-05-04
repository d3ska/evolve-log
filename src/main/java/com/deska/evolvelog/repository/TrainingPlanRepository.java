package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.TrainingPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

    @EntityGraph(attributePaths = {"plannedExercises", "block"})
    List<TrainingPlan> findByUserIdOrderByCreatedAtAsc(UUID userId);

    @EntityGraph(attributePaths = {"plannedExercises", "block"})
    @Query("SELECT p FROM TrainingPlan p WHERE p.id = :id AND p.user.id = :userId")
    Optional<TrainingPlan> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM TrainingPlan p WHERE p.block.id = :blockId AND p.user.id = :userId")
    void deleteByBlockIdAndUserId(@Param("blockId") UUID blockId, @Param("userId") UUID userId);
}
