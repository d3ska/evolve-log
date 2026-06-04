package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.ExerciseDefinitionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseDefinitionTranslationRepository
        extends JpaRepository<ExerciseDefinitionTranslation, ExerciseDefinitionTranslation.TranslationId> {

    Optional<ExerciseDefinitionTranslation> findByIdExerciseDefinitionIdAndIdLocale(UUID exerciseDefinitionId, String locale);

    List<ExerciseDefinitionTranslation> findByIdLocale(String locale);
}
