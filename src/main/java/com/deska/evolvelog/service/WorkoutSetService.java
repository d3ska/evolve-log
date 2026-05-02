package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.WorkoutSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutSetService {

    private final WorkoutSetRepository setRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutSet addSet(UUID exerciseId, UUID userId, Integer setNumber, Integer reps, BigDecimal weightKg) {
        var exercise = exerciseRepository.findByIdAndWorkoutSessionUserId(exerciseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));

        WorkoutSet ws = WorkoutSet.builder()
                .exercise(exercise)
                .setNumber(setNumber)
                .reps(reps)
                .weightKg(weightKg)
                .build();

        return setRepository.save(ws);
    }

    @Transactional
    public WorkoutSet updateSet(UUID setId, UUID userId, Integer reps, BigDecimal weightKg, Boolean completed) {
        WorkoutSet ws = setRepository.findByIdAndUserId(setId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSet", setId));
        ws.update(reps, weightKg, completed);
        return setRepository.save(ws);
    }

    @Transactional
    public void deleteSet(UUID setId, UUID userId) {
        WorkoutSet ws = setRepository.findByIdAndUserId(setId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutSet", setId));
        setRepository.delete(ws);
    }
}
