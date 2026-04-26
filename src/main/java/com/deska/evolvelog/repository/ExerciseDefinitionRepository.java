package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.ExerciseDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseDefinitionRepository extends JpaRepository<ExerciseDefinition, UUID> {

    Optional<ExerciseDefinition> findByNameIgnoreCaseAndIsSystemTrue(String name);

    @Query("""
            SELECT d FROM ExerciseDefinition d
            WHERE d.isSystem = true
               OR d.userId = :userId
            """)
    List<ExerciseDefinition> findByUserIdOrIsSystemTrue(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT d.primaryMuscle
            FROM ExerciseDefinition d
            ORDER BY d.primaryMuscle ASC
            """)
    List<String> findDistinctPrimaryMuscles();

    @Query("""
            SELECT d FROM ExerciseDefinition d
            WHERE d.id = :id
              AND (d.isSystem = true OR d.userId = :userId)
            """)
    Optional<ExerciseDefinition> findByIdAccessibleToUser(@Param("id") UUID id, @Param("userId") UUID userId);
}
