package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.ExerciseDefinitionTranslation;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateExerciseDefinitionRequest;
import com.deska.evolvelog.dto.response.ExerciseDefinitionDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.i18n.LocaleContextHolder;
import com.deska.evolvelog.i18n.MuscleI18n;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseDefinitionTranslationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExerciseDefinitionService {

    private final ExerciseDefinitionRepository repository;
    private final ExerciseDefinitionTranslationRepository translationRepository;
    private final LocaleContextHolder localeContextHolder;

    public ExerciseDefinitionService(ExerciseDefinitionRepository repository,
                                     ExerciseDefinitionTranslationRepository translationRepository,
                                     LocaleContextHolder localeContextHolder) {
        this.repository = repository;
        this.translationRepository = translationRepository;
        this.localeContextHolder = localeContextHolder;
    }

    @Transactional(readOnly = true)
    public List<ExerciseDefinitionDto> listDefinitions(UUID userId, String query, String muscle) {
        String locale = localeContextHolder.getLocale();
        List<ExerciseDefinition> definitions = repository.findByUserIdOrIsSystemTrue(userId);
        Map<UUID, ExerciseDefinitionTranslation> translations = buildTranslationMap(locale);

        return definitions.stream()
                .filter(d -> query == null || resolvedName(d, translations).toLowerCase().contains(query.toLowerCase()))
                .filter(d -> muscle == null || d.getPrimaryMuscle().equalsIgnoreCase(muscle))
                .map(d -> toDto(d, translations, locale))
                .toList();
    }

    @Transactional
    public ExerciseDefinitionDto createUserDefinition(User user, CreateExerciseDefinitionRequest request) {
        if (repository.findByNameIgnoreCaseAndIsSystemTrue(request.name()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A system exercise with name '" + request.name() + "' already exists");
        }

        ExerciseDefinition definition = ExerciseDefinition.builder()
                .name(request.name())
                .primaryMuscle(request.primaryMuscle())
                .secondaryMuscles(List.of())
                .equipment(request.equipment())
                .isSystem(false)
                .userId(user.getId())
                .build();

        String locale = localeContextHolder.getLocale();
        return toDto(repository.save(definition), Map.of(), locale);
    }

    @Transactional(readOnly = true)
    public List<String> getMuscleGroups() {
        return repository.findDistinctPrimaryMuscles();
    }

    private Map<UUID, ExerciseDefinitionTranslation> buildTranslationMap(String locale) {
        return translationRepository.findByIdLocale(locale).stream()
                .collect(Collectors.toMap(t -> t.getId().exerciseDefinitionId(), t -> t));
    }

    private String resolvedName(ExerciseDefinition d, Map<UUID, ExerciseDefinitionTranslation> translations) {
        if (d.isSystem()) {
            ExerciseDefinitionTranslation t = translations.get(d.getId());
            if (t != null) return t.getName();
        }
        return d.getName();
    }

    private ExerciseDefinitionDto toDto(ExerciseDefinition d,
                                        Map<UUID, ExerciseDefinitionTranslation> translations,
                                        String locale) {
        String name = resolvedName(d, translations);
        return new ExerciseDefinitionDto(
                d.getId(),
                name,
                d.getPrimaryMuscle(),
                MuscleI18n.label(d.getPrimaryMuscle(), locale),
                d.getSecondaryMuscles() != null ? d.getSecondaryMuscles() : List.of(),
                d.getEquipment(),
                d.isSystem()
        );
    }
}
