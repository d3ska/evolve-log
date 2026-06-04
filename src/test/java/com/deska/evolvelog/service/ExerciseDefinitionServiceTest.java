package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.ExerciseDefinitionTranslation;
import com.deska.evolvelog.dto.response.ExerciseDefinitionDto;
import com.deska.evolvelog.i18n.LocaleContextHolder;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseDefinitionTranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseDefinitionServiceTest {

    @Mock private ExerciseDefinitionRepository repository;
    @Mock private ExerciseDefinitionTranslationRepository translationRepository;
    @Mock private LocaleContextHolder localeContextHolder;

    @InjectMocks
    private ExerciseDefinitionService service;

    private UUID userId;
    private UUID definitionId;
    private ExerciseDefinition benchPress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        definitionId = UUID.randomUUID();
        benchPress = ExerciseDefinition.builder()
                .id(definitionId)
                .name("Barbell Bench Press")
                .primaryMuscle("chest")
                .secondaryMuscles(List.of())
                .equipment("barbell")
                .isSystem(true)
                .build();
    }

    // T15 — PL user → Polish names returned

    @Test
    void shouldReturnPolishNameWhenLocaleIsPlAndTranslationExists() {
        // given
        ExerciseDefinitionTranslation plTranslation = ExerciseDefinitionTranslation.builder()
                .id(new ExerciseDefinitionTranslation.TranslationId(definitionId, "pl"))
                .name("Wyciskanie sztangi na ławce poziomej")
                .build();

        when(localeContextHolder.getLocale()).thenReturn("pl");
        when(repository.findByUserIdOrIsSystemTrue(userId)).thenReturn(List.of(benchPress));
        when(translationRepository.findByIdLocale("pl")).thenReturn(List.of(plTranslation));

        // when
        List<ExerciseDefinitionDto> result = service.listDefinitions(userId, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Wyciskanie sztangi na ławce poziomej");
        assertThat(result.get(0).primaryMuscleLabel()).isEqualTo("Klatka piersiowa");
    }

    // T16 — EN user → English names returned

    @Test
    void shouldReturnEnglishNameWhenLocaleIsEn() {
        // given
        ExerciseDefinitionTranslation enTranslation = ExerciseDefinitionTranslation.builder()
                .id(new ExerciseDefinitionTranslation.TranslationId(definitionId, "en"))
                .name("Barbell Bench Press")
                .build();

        when(localeContextHolder.getLocale()).thenReturn("en");
        when(repository.findByUserIdOrIsSystemTrue(userId)).thenReturn(List.of(benchPress));
        when(translationRepository.findByIdLocale("en")).thenReturn(List.of(enTranslation));

        // when
        List<ExerciseDefinitionDto> result = service.listDefinitions(userId, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Barbell Bench Press");
        assertThat(result.get(0).primaryMuscleLabel()).isEqualTo("Chest");
    }

    // T17 — exercise with no PL translation → falls back to raw EN name

    @Test
    void shouldFallBackToRawNameWhenNoTranslationExistsForLocale() {
        // given — PL locale requested but no PL translation exists for this exercise
        when(localeContextHolder.getLocale()).thenReturn("pl");
        when(repository.findByUserIdOrIsSystemTrue(userId)).thenReturn(List.of(benchPress));
        when(translationRepository.findByIdLocale("pl")).thenReturn(List.of());

        // when
        List<ExerciseDefinitionDto> result = service.listDefinitions(userId, null, null);

        // then — raw name from exercise_definitions is used
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Barbell Bench Press");
    }

    @Test
    void shouldUseRawNameForUserCreatedExerciseRegardlessOfLocale() {
        // given
        UUID userExerciseId = UUID.randomUUID();
        ExerciseDefinition userExercise = ExerciseDefinition.builder()
                .id(userExerciseId)
                .name("My Custom Exercise")
                .primaryMuscle("chest")
                .secondaryMuscles(List.of())
                .isSystem(false)
                .userId(userId)
                .build();
        ExerciseDefinitionTranslation plTranslation = ExerciseDefinitionTranslation.builder()
                .id(new ExerciseDefinitionTranslation.TranslationId(userExerciseId, "pl"))
                .name("Should not be used")
                .build();

        when(localeContextHolder.getLocale()).thenReturn("pl");
        when(repository.findByUserIdOrIsSystemTrue(userId)).thenReturn(List.of(userExercise));
        when(translationRepository.findByIdLocale("pl")).thenReturn(List.of(plTranslation));

        // when
        List<ExerciseDefinitionDto> result = service.listDefinitions(userId, null, null);

        // then — user exercises always use their raw name
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("My Custom Exercise");
    }

    @Test
    void shouldFilterByQueryUsingResolvedName() {
        // given — searching in Polish but filter should match against the translated name
        ExerciseDefinitionTranslation plTranslation = ExerciseDefinitionTranslation.builder()
                .id(new ExerciseDefinitionTranslation.TranslationId(definitionId, "pl"))
                .name("Wyciskanie sztangi na ławce poziomej")
                .build();

        when(localeContextHolder.getLocale()).thenReturn("pl");
        when(repository.findByUserIdOrIsSystemTrue(userId)).thenReturn(List.of(benchPress));
        when(translationRepository.findByIdLocale("pl")).thenReturn(List.of(plTranslation));

        // when — query matches Polish name
        List<ExerciseDefinitionDto> matched = service.listDefinitions(userId, "wyciskanie", null);
        List<ExerciseDefinitionDto> unmatched = service.listDefinitions(userId, "squat", null);

        // then
        assertThat(matched).hasSize(1);
        assertThat(unmatched).isEmpty();
    }
}
