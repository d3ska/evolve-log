package com.deska.evolvelog.ai.prompt;

import com.deska.evolvelog.domain.AiSettings;
import com.deska.evolvelog.domain.FitatuFoodLog;
import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.SupplementPlan;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.repository.AiSettingsRepository;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.FitatuFoodLogRepository;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.deska.evolvelog.repository.SupplementPlanRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
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
    private static final String WITHINGS_SOURCE = "withings";

    private final WorkoutSessionRepository workoutSessionRepository;
    private final BloodTestReportRepository bloodTestReportRepository;
    private final MeasurementRepository measurementRepository;
    private final FitatuFoodLogRepository fitatuFoodLogRepository;
    private final HealthMetricService healthMetricService;
    private final AiSettingsRepository aiSettingsRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final SupplementPlanRepository supplementPlanRepository;

    public PromptContextBuilder(WorkoutSessionRepository workoutSessionRepository,
                                BloodTestReportRepository bloodTestReportRepository,
                                MeasurementRepository measurementRepository,
                                FitatuFoodLogRepository fitatuFoodLogRepository,
                                HealthMetricService healthMetricService,
                                AiSettingsRepository aiSettingsRepository,
                                TrainingPlanRepository trainingPlanRepository,
                                SupplementPlanRepository supplementPlanRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.bloodTestReportRepository = bloodTestReportRepository;
        this.measurementRepository = measurementRepository;
        this.fitatuFoodLogRepository = fitatuFoodLogRepository;
        this.healthMetricService = healthMetricService;
        this.aiSettingsRepository = aiSettingsRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.supplementPlanRepository = supplementPlanRepository;
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
        StringBuilder sb = new StringBuilder();

        // Prepend goals section if the user has set any
        aiSettingsRepository.findById(userId)
                .map(AiSettings::getGoals)
                .filter(g -> g != null && !g.isBlank())
                .ifPresent(goals -> sb.append("## Your Goals\n").append(goals).append("\n\n"));

        sb.append("## Available Data\n");

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

        List<TrainingPlan> allPlans = trainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId);
        long activePlanCount = allPlans.stream().filter(TrainingPlan::isActive).count();
        if (activePlanCount > 0) {
            sb.append("- **Training plans**: ").append(activePlanCount)
              .append(" active plan(s) — use get_training_plan to read exercises and structure\n");
        } else if (!allPlans.isEmpty()) {
            sb.append("- **Training plans**: ").append(allPlans.size()).append(" plan(s), none currently active\n");
        } else {
            sb.append("- **Training plans**: none created yet\n");
        }

        long activeSupplementPlanCount = supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(SupplementPlan::isActive).count();
        if (activeSupplementPlanCount > 0) {
            sb.append("- **Supplements**: ").append(activeSupplementPlanCount)
              .append(" active plan(s) — use get_supplement_info to read protocol\n");
        } else {
            sb.append("- **Supplements**: no active supplement plan\n");
        }

        LocalDate withingsCutoff = LocalDate.now().minusDays(30);
        List<DailyHealthMetricsDto> withingsData = healthMetricService
                .getDailyMetrics(userId, WITHINGS_SOURCE, withingsCutoff, LocalDate.now());
        if (!withingsData.isEmpty()) {
            LocalDate lastWithingsDay = withingsData.getLast().date();
            sb.append("- **Body metrics (Withings)**: ").append(withingsData.size())
              .append(" days in last 30 days, last sync on ")
              .append(lastWithingsDay.format(DATE_FMT))
              .append(" — use get_health_metrics to read weight, BMR, body fat, VO2max etc.\n");
        } else {
            sb.append("- **Body metrics (Withings)**: no scale data synced yet\n");
        }

        if (pageContext != null && !pageContext.isBlank()) {
            sb.append("\n*User is currently viewing the **").append(pageContext).append("** page.*\n");
        }

        return sb.toString();
    }
}
