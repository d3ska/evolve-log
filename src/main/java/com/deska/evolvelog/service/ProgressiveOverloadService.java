package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.ExerciseDefinition;
import com.deska.evolvelog.dto.response.OverloadHistoryEntryDto;
import com.deska.evolvelog.dto.response.ProgressiveOverloadDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.util.VolumeCalculator;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressiveOverloadService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseDefinitionRepository definitionRepository;

    public ProgressiveOverloadService(ExerciseRepository exerciseRepository,
                                      ExerciseDefinitionRepository definitionRepository) {
        this.exerciseRepository = exerciseRepository;
        this.definitionRepository = definitionRepository;
    }

    @Transactional(readOnly = true)
    public ProgressiveOverloadDto getProgressiveOverload(UUID userId, UUID definitionId, int sessions) {
        ExerciseDefinition definition = definitionRepository.findById(definitionId)
                .filter(d -> d.isSystem() || userId.equals(d.getUserId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exercise definition not found"));

        // Fetch most recent `sessions` sessions (DESC), then reverse for oldest-first PR scan
        List<Exercise> descList = exerciseRepository.findByUserIdAndDefinitionId(
                userId, definitionId, PageRequest.of(0, sessions));

        if (descList.isEmpty()) {
            return new ProgressiveOverloadDto(definitionId, definition.getName(), List.of());
        }

        // Reverse to oldest-first for PR detection
        List<Exercise> ascList = new ArrayList<>(descList.reversed());

        BigDecimal runningMaxE1rm = null;
        List<OverloadHistoryEntryDto> entries = new ArrayList<>(ascList.size());

        for (Exercise e : ascList) {
            Integer reps = e.getReps();
            Integer sets = e.getSets();
            BigDecimal e1rm = (reps != null) ? VolumeCalculator.epleyE1RM(e.getWeightKg(), reps) : null;
            BigDecimal performanceIndicator = e1rm != null ? e1rm : e.getWeightKg();
            BigDecimal vl = (sets != null && reps != null) ? VolumeCalculator.volumeLoad(sets, reps, e.getWeightKg()) : null;

            boolean isPR;
            if (runningMaxE1rm == null) {
                isPR = true;
            } else {
                isPR = performanceIndicator != null && performanceIndicator.compareTo(runningMaxE1rm) > 0;
            }

            if (performanceIndicator != null && (runningMaxE1rm == null || performanceIndicator.compareTo(runningMaxE1rm) > 0)) {
                runningMaxE1rm = performanceIndicator;
            }

            entries.add(new OverloadHistoryEntryDto(
                    e.getWorkoutSession().getDate().toLocalDate(),
                    e.getWorkoutSession().getId(),
                    e.getSets(),
                    e.getReps(),
                    e.getWeightKg(),
                    e1rm,
                    vl,
                    e.getRpe(),
                    isPR,
                    null // volumeDelta filled in next pass
            ));
        }

        // Fill volumeDelta (oldest entry stays null)
        List<OverloadHistoryEntryDto> withDeltas = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            OverloadHistoryEntryDto current = entries.get(i);
            BigDecimal delta = null;
            if (i > 0) {
                BigDecimal prevVl = entries.get(i - 1).volumeLoad();
                BigDecimal currVl = current.volumeLoad();
                if (prevVl != null && currVl != null) {
                    delta = currVl.subtract(prevVl);
                }
            }
            withDeltas.add(new OverloadHistoryEntryDto(
                    current.sessionDate(), current.sessionId(), current.sets(), current.reps(),
                    current.weightKg(), current.e1Rm(), current.volumeLoad(), current.rpe(),
                    current.isPR(), delta));
        }

        // Return most-recent-first as specified
        return new ProgressiveOverloadDto(definitionId, definition.getName(), withDeltas.reversed());
    }
}
