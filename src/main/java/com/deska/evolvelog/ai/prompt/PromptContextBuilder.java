package com.deska.evolvelog.ai.prompt;

import com.deska.evolvelog.domain.FitatuFoodLog;
import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.FitatuFoodLogRepository;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import com.deska.evolvelog.service.HealthMetricService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Builds a lightweight data-availability summary injected into the AI system prompt.
 * Does NOT pre-load full datasets — the AI uses tools to fetch actual data on demand.
 */
@Component
public class PromptContextBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String FITATU_SOURCE = "fitatu";

    private final WorkoutSessionRepository workoutSessionRepository;
    private final BloodTestReportRepository bloodTestReportRepository;
    private final MeasurementRepository measurementRepository;
    private final FitatuFoodLogRepository fitatuFoodLogRepository;
    private final HealthMetricService healthMetricService;

    public PromptContextBuilder(WorkoutSessionRepository workoutSessionRepository,
                                BloodTestReportRepository bloodTestReportRepository,
                                MeasurementRepository measurementRepository,
                                FitatuFoodLogRepository fitatuFoodLogRepository,
                                HealthMetricService healthMetricService) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.bloodTestReportRepository = bloodTestReportRepository;
        this.measurementRepository = measurementRepository;
        this.fitatuFoodLogRepository = fitatuFoodLogRepository;
        this.healthMetricService = healthMetricService;
    }

    /**
     * Returns a brief summary of what data is available for this user.
     * The AI reads this to know which tools are worth calling — it does not contain the actual data.
     *
     * @param pageContext the page the user is currently viewing (used as a hint, not to load data)
     * @param userId      the authenticated user's ID
     */
    @Transactional(readOnly = true)
    public String buildContext(String pageContext, UUID userId) {
        StringBuilder sb = new StringBuilder("## Available Data\n");

        WorkoutSession lastWorkout = workoutSessionRepository
                .findByUserIdOrderByDateDesc(userId, PageRequest.of(0, 1))
                .getContent().stream().findFirst().orElse(null);
        if (lastWorkout != null) {
            sb.append("- **Workouts**: recorded, last session on ")
              .append(lastWorkout.getDate().format(DATE_FMT)).append("\n");
        } else {
            sb.append("- **Workouts**: no sessions recorded yet\n");
        }

        Measurement lastMeasurement = measurementRepository.findFirstByUserIdOrderByDateDesc(userId).orElse(null);
        if (lastMeasurement != null) {
            sb.append("- **Body measurements**: recorded, last entry on ")
              .append(lastMeasurement.getDate().format(DATE_FMT)).append("\n");
        } else {
            sb.append("- **Body measurements**: none recorded yet\n");
        }

        long bloodReportCount = bloodTestReportRepository.countByUserId(userId);
        if (bloodReportCount > 0) {
            sb.append("- **Blood test reports**: ").append(bloodReportCount).append(" report(s) available\n");
        } else {
            sb.append("- **Blood tests**: no reports uploaded yet\n");
        }

        LocalDate nutritionCutoff = LocalDate.now().minusDays(30);
        List<FitatuFoodLog> recentNutrition = fitatuFoodLogRepository
                .findByUserIdAndDateBetween(userId, nutritionCutoff, LocalDate.now());
        if (!recentNutrition.isEmpty()) {
            LocalDate lastNutritionDay = recentNutrition.get(0).getDate();
            sb.append("- **Nutrition log (Fitatu)**: ").append(recentNutrition.stream()
                    .map(FitatuFoodLog::getDate).distinct().count())
              .append(" days in last 30 days, last entry on ")
              .append(lastNutritionDay.format(DATE_FMT)).append("\n");
        } else {
            // Fallback: check health_metrics (daily aggregates saved during CSV import)
            List<DailyHealthMetricsDto> aggregates = healthMetricService
                    .getDailyMetrics(userId, FITATU_SOURCE, nutritionCutoff, LocalDate.now());
            if (!aggregates.isEmpty()) {
                LocalDate lastDay = aggregates.getLast().date();
                sb.append("- **Nutrition log (Fitatu)**: ").append(aggregates.size())
                  .append(" days in last 30 days, last entry on ")
                  .append(lastDay.format(DATE_FMT)).append("\n");
            } else {
                sb.append("- **Nutrition log**: no Fitatu data imported yet\n");
            }
        }

        if (pageContext != null && !pageContext.isBlank()) {
            sb.append("\n*User is currently viewing the **").append(pageContext).append("** page.*\n");
        }

        return sb.toString();
    }
}
