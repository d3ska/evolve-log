package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateExerciseDefinitionRequest;
import com.deska.evolvelog.dto.response.ExerciseDefinitionDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExerciseDefinitionService {

    private final ExerciseDefinitionRepository repository;

    public ExerciseDefinitionService(ExerciseDefinitionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ExerciseDefinitionDto> listDefinitions(UUID userId, String query, String muscle) {
        return repository.findByUserIdOrIsSystemTrue(userId).stream()
                .filter(d -> query == null || d.getName().toLowerCase().contains(query.toLowerCase()))
                .filter(d -> muscle == null || d.getPrimaryMuscle().equalsIgnoreCase(muscle))
                .map(this::toDto)
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

        return toDto(repository.save(definition));
    }

    @Transactional(readOnly = true)
    public List<String> getMuscleGroups() {
        return repository.findDistinctPrimaryMuscles();
    }

    private ExerciseDefinitionDto toDto(ExerciseDefinition d) {
        return new ExerciseDefinitionDto(
                d.getId(),
                d.getName(),
                d.getPrimaryMuscle(),
                d.getSecondaryMuscles() != null ? d.getSecondaryMuscles() : List.of(),
                d.getEquipment(),
                d.isSystem()
        );
    }
}
