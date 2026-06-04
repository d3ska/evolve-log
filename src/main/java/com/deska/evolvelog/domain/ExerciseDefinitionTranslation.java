package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "exercise_definition_translations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseDefinitionTranslation {

    @EmbeddedId
    private TranslationId id;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Embeddable
    public record TranslationId(
            @Column(name = "exercise_definition_id", nullable = false)
            UUID exerciseDefinitionId,

            @Column(name = "locale", nullable = false, length = 10)
            String locale
    ) implements Serializable {}
}
