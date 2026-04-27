package com.deska.evolvelog.scheduler;

import com.deska.evolvelog.ai.router.AiTaskType;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.repository.MonthlyAggregateRepository;
import com.deska.evolvelog.repository.UserRepository;
import com.deska.evolvelog.service.AiInsightService;
import com.deska.evolvelog.service.AiSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InsightScheduler {

    private static final Logger log = LoggerFactory.getLogger(InsightScheduler.class);

    private final AiInsightService aiInsightService;
    private final AiSettingsService aiSettingsService;
    private final MonthlyAggregateRepository monthlyAggregateRepository;
    private final UserRepository userRepository;

    public InsightScheduler(AiInsightService aiInsightService,
                            AiSettingsService aiSettingsService,
                            MonthlyAggregateRepository monthlyAggregateRepository,
                            UserRepository userRepository) {
        this.aiInsightService = aiInsightService;
        this.aiSettingsService = aiSettingsService;
        this.monthlyAggregateRepository = monthlyAggregateRepository;
        this.userRepository = userRepository;
    }

    /** Daily summary — runs at 02:00 every day. */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySummary() {
        log.info("Starting daily summary insight job");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            boolean hasKey = aiSettingsService.getDecryptedApiKey(user.getId())
                    .filter(k -> !k.isBlank())
                    .isPresent();
            if (!hasKey) {
                log.info("Skipping daily summary for user {} — no API key configured", user.getId());
                continue;
            }
            try {
                aiInsightService.generateInsight(user.getId(), AiTaskType.DAILY_SUMMARY);
                log.info("Daily summary generated for user {}", user.getId());
            } catch (Exception e) {
                log.error("Failed to generate daily summary for user {}", user.getId(), e);
            }
        }
    }

    /** Weekly report — runs at 03:00 every Monday. */
    @Scheduled(cron = "0 0 3 * * MON")
    public void runWeeklyReport() {
        log.info("Starting weekly report insight job");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            boolean hasKey = aiSettingsService.getDecryptedApiKey(user.getId())
                    .filter(k -> !k.isBlank())
                    .isPresent();
            if (!hasKey) {
                log.info("Skipping weekly report for user {} — no API key configured", user.getId());
                continue;
            }
            try {
                aiInsightService.generateInsight(user.getId(), AiTaskType.WEEKLY_REPORT);
                log.info("Weekly report generated for user {}", user.getId());
            } catch (Exception e) {
                log.error("Failed to generate weekly report for user {}", user.getId(), e);
            }
        }
    }

    /** Monthly aggregate refresh — runs at 02:30 every day. */
    @Scheduled(cron = "0 30 2 * * *")
    public void runMonthlyAggregates() {
        log.info("Starting monthly exercise aggregate job");
        try {
            monthlyAggregateRepository.upsertAllFromExerciseSets();
            log.info("Monthly exercise aggregates refreshed");
        } catch (Exception e) {
            log.error("Failed to refresh monthly exercise aggregates", e);
        }
    }
}
