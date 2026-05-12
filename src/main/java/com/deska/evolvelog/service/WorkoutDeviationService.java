package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.DeviationEntryDto;
import com.deska.evolvelog.dto.response.PlanSnapshotEntry;
import com.deska.evolvelog.dto.response.SessionDeviationDto;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutDeviationService {

    private final WorkoutSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SessionDeviationDto getDeviations(UUID sessionId, UUID userId) {
        WorkoutSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSession", sessionId));

        if (session.getPlanSnapshot() == null) {
            return new SessionDeviationDto(sessionId, false, List.of());
        }

        List<PlanSnapshotEntry> snapshot = deserializeSnapshot(session.getPlanSnapshot());
        List<Exercise> exercises = session.getExercises();

        // Build map: plannedExerciseId -> Exercise (only exercises with a non-null plannedExerciseId)
        Map<UUID, Exercise> byPlannedId = exercises.stream()
                .filter(e -> e.getPlannedExerciseId() != null)
                .collect(Collectors.toMap(Exercise::getPlannedExerciseId, Function.identity()));

        List<DeviationEntryDto> entries = new ArrayList<>();

        // Process snapshot entries: COMPLETED or SKIPPED
        for (PlanSnapshotEntry snapshotEntry : snapshot) {
            Exercise match = byPlannedId.get(snapshotEntry.plannedExerciseId());
            if (match != null) {
                entries.add(new DeviationEntryDto(
                        "COMPLETED",
                        snapshotEntry.plannedExerciseId(),
                        snapshotEntry.name(),
                        snapshotEntry.sets(),
                        snapshotEntry.repsMin(),
                        snapshotEntry.repsMax(),
                        match.getSets(),
                        match.getId()
                ));
            } else {
                // SKIPPED: snapshot entry has no matching session exercise
                entries.add(new DeviationEntryDto(
                        "SKIPPED",
                        snapshotEntry.plannedExerciseId(),
                        snapshotEntry.name(),
                        snapshotEntry.sets(),
                        snapshotEntry.repsMin(),
                        snapshotEntry.repsMax(),
                        null,
                        null
                ));
            }
        }

        // ADDED: exercises with null plannedExerciseId (unplanned additions)
        for (Exercise exercise : exercises) {
            if (exercise.getPlannedExerciseId() == null) {
                entries.add(new DeviationEntryDto(
                        "ADDED",
                        null,
                        exercise.getName(),
                        null,
                        null,
                        null,
                        exercise.getSets(),
                        exercise.getId()
                ));
            }
        }

        return new SessionDeviationDto(sessionId, true, entries);
    }

    private List<PlanSnapshotEntry> deserializeSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PlanSnapshotEntry>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize plan snapshot", e);
        }
    }
}
