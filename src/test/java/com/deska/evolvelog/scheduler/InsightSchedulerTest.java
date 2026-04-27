package com.deska.evolvelog.scheduler;

import com.deska.evolvelog.ai.router.AiTaskType;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.repository.MonthlyAggregateRepository;
import com.deska.evolvelog.repository.UserRepository;
import com.deska.evolvelog.service.AiInsightService;
import com.deska.evolvelog.service.AiSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightSchedulerTest {

    @Mock
    private AiInsightService aiInsightService;

    @Mock
    private AiSettingsService aiSettingsService;

    @Mock
    private MonthlyAggregateRepository monthlyAggregateRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InsightScheduler insightScheduler;

    private User buildUser(UUID id) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- daily summary ---

    @Test
    void shouldSkipDailySummaryWhenUserHasNoApiKey() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findAll()).thenReturn(List.of(buildUser(userId)));
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.empty());

        // when
        insightScheduler.runDailySummary();

        // then
        verify(aiInsightService, never()).generateInsight(any(), any());
    }

    @Test
    void shouldSkipDailySummaryWhenUserHasBlankApiKey() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findAll()).thenReturn(List.of(buildUser(userId)));
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("   "));

        // when
        insightScheduler.runDailySummary();

        // then
        verify(aiInsightService, never()).generateInsight(any(), any());
    }

    @Test
    void shouldGenerateDailySummaryWithCorrectTaskType() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findAll()).thenReturn(List.of(buildUser(userId)));
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("sk-valid-key"));

        // when
        insightScheduler.runDailySummary();

        // then
        verify(aiInsightService).generateInsight(eq(userId), eq(AiTaskType.DAILY_SUMMARY));
    }

    @Test
    void shouldContinueWithOtherUsersDailySummaryWhenOneThrows() {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = buildUser(userId1);
        User user2 = buildUser(userId2);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(aiSettingsService.getDecryptedApiKey(userId1)).thenReturn(Optional.of("sk-key-1"));
        when(aiSettingsService.getDecryptedApiKey(userId2)).thenReturn(Optional.of("sk-key-2"));
        doThrow(new RuntimeException("AI error")).when(aiInsightService).generateInsight(eq(userId1), any());

        // when
        insightScheduler.runDailySummary();

        // then
        verify(aiInsightService).generateInsight(eq(userId2), eq(AiTaskType.DAILY_SUMMARY));
    }

    // --- weekly report ---

    @Test
    void shouldSkipWeeklyReportWhenUserHasNoApiKey() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findAll()).thenReturn(List.of(buildUser(userId)));
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.empty());

        // when
        insightScheduler.runWeeklyReport();

        // then
        verify(aiInsightService, never()).generateInsight(any(), any());
    }

    @Test
    void shouldGenerateWeeklyReportWithCorrectTaskType() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findAll()).thenReturn(List.of(buildUser(userId)));
        when(aiSettingsService.getDecryptedApiKey(userId)).thenReturn(Optional.of("sk-valid-key"));

        // when
        insightScheduler.runWeeklyReport();

        // then
        verify(aiInsightService).generateInsight(eq(userId), eq(AiTaskType.WEEKLY_REPORT));
    }

    @Test
    void shouldContinueWithOtherUsersWeeklyReportWhenOneThrows() {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = buildUser(userId1);
        User user2 = buildUser(userId2);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(aiSettingsService.getDecryptedApiKey(userId1)).thenReturn(Optional.of("sk-key-1"));
        when(aiSettingsService.getDecryptedApiKey(userId2)).thenReturn(Optional.of("sk-key-2"));
        doThrow(new RuntimeException("AI error")).when(aiInsightService).generateInsight(eq(userId1), any());

        // when
        insightScheduler.runWeeklyReport();

        // then
        verify(aiInsightService).generateInsight(eq(userId2), eq(AiTaskType.WEEKLY_REPORT));
    }

    // --- monthly aggregates ---

    @Test
    void shouldCallUpsertAllWhenRunningMonthlyAggregates() {
        // when
        insightScheduler.runMonthlyAggregates();

        // then
        verify(monthlyAggregateRepository).upsertAllFromExerciseSets();
    }

    @Test
    void shouldNotPropagateExceptionFromMonthlyAggregates() {
        // given
        doThrow(new RuntimeException("DB error")).when(monthlyAggregateRepository).upsertAllFromExerciseSets();

        // when / then — must not throw
        insightScheduler.runMonthlyAggregates();
    }
}
