package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {
    Page<WorkoutSession> findByUserIdOrderByDateDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"exercises"})
    @Query("SELECT s FROM WorkoutSession s WHERE s.user.id = :userId ORDER BY s.date DESC")
    List<WorkoutSession> findRecentByUserIdWithExercises(@Param("userId") UUID userId, Pageable pageable);

    List<WorkoutSession> findByUserIdAndDateBetweenOrderByDateAsc(UUID userId, LocalDateTime start, LocalDateTime end);

    // Eagerly loads exercises; workoutSets loaded via SUBSELECT (FetchType.EAGER on Exercise)
    @EntityGraph(attributePaths = {"exercises"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT s FROM WorkoutSession s WHERE s.id = :id AND s.user.id = :userId")
    Optional<WorkoutSession> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUserIdAndStatus(UUID userId, WorkoutSessionStatus status);

    @EntityGraph(attributePaths = {"exercises"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<WorkoutSession> findByUserIdAndStatus(UUID userId, WorkoutSessionStatus status);
}
