package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.dto.response.MuscleGroupVolumeDto;
import com.deska.evolvelog.dto.response.SessionVolumeSummaryDto;
import com.deska.evolvelog.dto.response.WeeklyMuscleVolumeDto;
import com.deska.evolvelog.exception.ApiException;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.WeeklyVolumeRow;
import com.deska.evolvelog.util.VolumeCalculator;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TrainingVolumeService {

    private static final int MAX_WEEKS = 52;

    private final ExerciseRepository exerciseRepository;

    public TrainingVolumeService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public SessionVolumeSummaryDto getSessionVolumeSummary(UUID sessionId, UUID userId) {
        List<Exercise> exercises = exerciseRepository.findBySessionIdAndUserId(sessionId, userId);
        if (exercises.isEmpty()) {
            // Check if the session exists at all — if the query returns nothing, treat as 404
            throw new ApiException(HttpStatus.NOT_FOUND, "Session not found");
        }

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalInternal = BigDecimal.ZERO;
        int withRpe = 0;

        // Accumulate per-muscle stats
        Map<String, BigDecimal> muscleVolume = new LinkedHashMap<>();
        Map<String, BigDecimal> muscleInternal = new LinkedHashMap<>();
        Map<String, Integer> muscleCount = new LinkedHashMap<>();

        for (Exercise e : exercises) {
            BigDecimal vl = e.getWorkoutSets().stream()
                    .filter(s -> s.getReps() != null && s.getWeightKg() != null)
                    .map(s -> s.getWeightKg().multiply(BigDecimal.valueOf(s.getReps())))
                    .reduce(BigDecimal::add)
                    .orElse(null);
            // Fallback for manually logged exercises that have no workout_set rows
            if (vl == null && e.getSets() != null && e.getReps() != null && e.getWeightKg() != null) {
                vl = e.getWeightKg().multiply(BigDecimal.valueOf((long) e.getSets() * e.getReps()));
            }
            BigDecimal il = VolumeCalculator.internalLoad(vl, e.getRpe());

            if (vl != null) totalVolume = totalVolume.add(vl);
            if (il != null) {
                totalInternal = totalInternal.add(il);
                withRpe++;
            } else if (e.getRpe() != null && vl != null) {
                withRpe++;
            }

            String muscle = e.getPrimaryMuscle();
            if (muscle != null && vl != null) {
                muscleVolume.merge(muscle, vl, BigDecimal::add);
                if (il != null) muscleInternal.merge(muscle, il, BigDecimal::add);
                muscleCount.merge(muscle, 1, Integer::sum);
            }
        }

        double rpeCompleteness = exercises.isEmpty() ? 0.0 : (double) withRpe / exercises.size();

        List<MuscleGroupVolumeDto> byMuscle = muscleVolume.entrySet().stream()
                .map(entry -> new MuscleGroupVolumeDto(
                        entry.getKey(),
                        entry.getValue(),
                        muscleInternal.get(entry.getKey()),
                        muscleCount.getOrDefault(entry.getKey(), 0)))
                .toList();

        return new SessionVolumeSummaryDto(
                sessionId,
                totalVolume,
                totalInternal.compareTo(BigDecimal.ZERO) > 0 ? totalInternal : null,
                rpeCompleteness,
                exercises.size(),
                byMuscle
        );
    }

    @Transactional(readOnly = true)
    public List<WeeklyMuscleVolumeDto> getWeeklyVolumeByMuscle(UUID userId, LocalDate from, LocalDate to, String muscle) {
        long weeks = ChronoUnit.WEEKS.between(from, to);
        if (weeks > MAX_WEEKS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Date range must not exceed " + MAX_WEEKS + " weeks");
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<WeeklyVolumeRow> rows = exerciseRepository.findWeeklyVolumeByMuscle(userId, fromDt, toDt, muscle);
        return rows.stream()
                .map(r -> new WeeklyMuscleVolumeDto(r.getWeekStart(), r.getMuscle(), r.getVolumeLoad(), r.getSessionCount(), r.getSetCount()))
                .toList();
    }
}
