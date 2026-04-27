package com.deska.evolvelog.ai.prompt;

import com.deska.evolvelog.domain.BloodTestReport;
import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.ExerciseRepository;
import com.deska.evolvelog.repository.MeasurementRepository;
import com.deska.evolvelog.repository.WorkoutSessionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Builds a textual context block injected into AI system prompts via the {{context}} placeholder.
 * Each page context fetches only the data relevant to that page — no new queries, only existing repositories.
 */
@Component
public class PromptContextBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorkoutSessionRepository workoutSessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final BloodTestReportRepository bloodTestReportRepository;
    private final MeasurementRepository measurementRepository;

    public PromptContextBuilder(WorkoutSessionRepository workoutSessionRepository,
                                ExerciseRepository exerciseRepository,
                                BloodTestReportRepository bloodTestReportRepository,
                                MeasurementRepository measurementRepository) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.bloodTestReportRepository = bloodTestReportRepository;
        this.measurementRepository = measurementRepository;
    }

    /**
     * Returns a context string based on the current page the user is viewing.
     * Injects relevant data so the AI can answer page-specific questions without tool calls.
     *
     * @param pageContext one of: dashboard, workouts, progress, blood, measurements, insights, or empty
     * @param userId      the authenticated user's ID
     */
    @Transactional(readOnly = true)
    public String buildContext(String pageContext, UUID userId) {
        if (pageContext == null || pageContext.isBlank()) {
            return buildDashboardContext(userId);
        }
        return switch (pageContext.toLowerCase()) {
            case "workouts"     -> buildWorkoutsContext(userId);
            case "progress"     -> buildProgressContext(userId);
            case "blood"        -> buildBloodContext(userId);
            case "measurements" -> buildMeasurementsContext(userId);
            case "insights"     -> buildInsightsContext(userId);
            default             -> buildDashboardContext(userId);
        };
    }

    private String buildDashboardContext(UUID userId) {
        List<WorkoutSession> recentSessions = workoutSessionRepository
                .findByUserIdOrderByDateDesc(userId, PageRequest.of(0, 5))
                .getContent();
        Measurement latestMeasurement = measurementRepository
                .findFirstByUserIdOrderByDateDesc(userId)
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("## Recent Workouts (last 5)\n");
        appendSessions(sb, recentSessions);
        sb.append("\n## Latest Measurements\n");
        appendMeasurement(sb, latestMeasurement);
        return sb.toString();
    }

    private String buildWorkoutsContext(UUID userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<WorkoutSession> sessions = workoutSessionRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, thirtyDaysAgo, LocalDateTime.now());

        StringBuilder sb = new StringBuilder();
        sb.append("## Workouts (last 30 days)\n");
        appendSessions(sb, sessions);
        return sb.toString();
    }

    private String buildProgressContext(UUID userId) {
        List<String> exerciseNames = exerciseRepository.findDistinctExerciseNamesByUserId(userId);
        List<?> personalRecords = exerciseRepository.findPersonalRecordsByUserId(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("## Tracked Exercises\n");
        exerciseNames.forEach(name -> sb.append("- ").append(name).append("\n"));
        sb.append("\n## Personal Records\n");
        personalRecords.forEach(pr -> sb.append("- ").append(pr).append("\n"));
        return sb.toString();
    }

    private String buildBloodContext(UUID userId) {
        List<BloodTestReport> reports = bloodTestReportRepository.findByUserIdOrderByDateDesc(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("## Blood Test Reports\n");
        for (BloodTestReport report : reports) {
            sb.append("### Report: ").append(report.getDate().format(DATE_FMT));
            if (report.getLabName() != null) {
                sb.append(" (").append(report.getLabName()).append(")");
            }
            sb.append("\n");
            report.getResults().forEach(r -> {
                sb.append("- ").append(r.getParameterLabel())
                        .append(": ").append(r.getValue()).append(" ").append(r.getUnit() != null ? r.getUnit() : "");
                if (r.getRefLow() != null && r.getRefHigh() != null) {
                    sb.append(" [ref: ").append(r.getRefLow()).append("–").append(r.getRefHigh()).append("]");
                }
                if (r.getFlag() != null) {
                    sb.append(" ⚠ ").append(r.getFlag());
                }
                sb.append("\n");
            });
        }
        return sb.toString();
    }

    private String buildMeasurementsContext(UUID userId) {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Measurement> measurements = measurementRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId,
                        sixMonthsAgo.toLocalDate(),
                        LocalDateTime.now().toLocalDate());

        StringBuilder sb = new StringBuilder();
        sb.append("## Body Measurements (last 6 months)\n");
        for (Measurement m : measurements) {
            sb.append("- ").append(m.getDate().format(DATE_FMT));
            if (m.getWeightKg() != null)       sb.append(", weight: ").append(m.getWeightKg()).append(" kg");
            if (m.getBodyFatPercent() != null)  sb.append(", body fat: ").append(m.getBodyFatPercent()).append("%");
            if (m.getWaistNavelCm() != null)    sb.append(", waist: ").append(m.getWaistNavelCm()).append(" cm");
            if (m.getBicepsCm() != null)        sb.append(", biceps: ").append(m.getBicepsCm()).append(" cm");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildInsightsContext(UUID userId) {
        // Insights page — provide recent workouts and measurements as background
        return buildDashboardContext(userId);
    }

    private void appendSessions(StringBuilder sb, List<WorkoutSession> sessions) {
        if (sessions.isEmpty()) {
            sb.append("No workouts recorded.\n");
            return;
        }
        for (WorkoutSession session : sessions) {
            sb.append("- ").append(session.getDate().format(DATETIME_FMT));
            if (session.getDurationMinutes() != null) {
                sb.append(", ").append(session.getDurationMinutes()).append(" min");
            }
            List<Exercise> exercises = session.getExercises();
            if (!exercises.isEmpty()) {
                sb.append(": ");
                for (int i = 0; i < Math.min(exercises.size(), 5); i++) {
                    Exercise e = exercises.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append(e.getName());
                    if (e.getSets() != null) sb.append(" ").append(e.getSets()).append("×");
                    if (e.getReps() != null) sb.append(e.getReps());
                    if (e.getWeightKg() != null) sb.append("@").append(e.getWeightKg()).append("kg");
                }
                if (exercises.size() > 5) sb.append(", +").append(exercises.size() - 5).append(" more");
            }
            sb.append("\n");
        }
    }

    private void appendMeasurement(StringBuilder sb, Measurement m) {
        if (m == null) {
            sb.append("No measurements recorded.\n");
            return;
        }
        sb.append("Date: ").append(m.getDate().format(DATE_FMT)).append("\n");
        if (m.getWeightKg() != null)       sb.append("- Weight: ").append(m.getWeightKg()).append(" kg\n");
        if (m.getBodyFatPercent() != null)  sb.append("- Body fat: ").append(m.getBodyFatPercent()).append("%\n");
        if (m.getWaistNavelCm() != null)    sb.append("- Waist (navel): ").append(m.getWaistNavelCm()).append(" cm\n");
        if (m.getBicepsCm() != null)        sb.append("- Biceps: ").append(m.getBicepsCm()).append(" cm\n");
    }
}
