package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.*;
import com.deska.evolvelog.dto.response.SupplementTodayDto;
import com.deska.evolvelog.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SupplementTodayStatusTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SupplementLogService logService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SupplementRepository supplementRepository;

    @Autowired
    SupplementPlanRepository planRepository;

    @Autowired
    SupplementPlanEntryRepository entryRepository;

    @Autowired
    SupplementLogRepository logRepository;

    private User testUser;
    private Supplement supplement;
    private SupplementPlan activePlan;
    private SupplementPlanEntry planEntry;

    @BeforeEach
    void setUp() {
        logRepository.deleteAll();
        entryRepository.deleteAll();
        planRepository.deleteAll();
        supplementRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test-supplement-today@example.com")
                .build());

        supplement = supplementRepository.save(Supplement.builder()
                .user(testUser)
                .name("Magnesium")
                .build());

        activePlan = planRepository.save(SupplementPlan.builder()
                .user(testUser)
                .name("Morning Routine")
                .active(true)
                .build());

        planEntry = entryRepository.save(SupplementPlanEntry.builder()
                .plan(activePlan)
                .supplement(supplement)
                .timeSlot(TimeSlot.MORNING)
                .doseAmount(new BigDecimal("400"))
                .doseUnit("mg")
                .build());
    }

    // ── T5: entry logged today → takenToday = true ───────────────────────────

    @Test
    void whenLoggedToday_thenTakenTodayIsTrue() {
        logRepository.save(SupplementLog.builder()
                .user(testUser)
                .supplement(supplement)
                .planEntry(planEntry)
                .takenAt(OffsetDateTime.now(ZoneOffset.UTC))
                .source(SupplementSource.PLANNED)
                .build());

        List<SupplementTodayDto> result = logService.getTodayStatus(testUser.getId());

        assertThat(result).hasSize(1);
        SupplementTodayDto plan = result.get(0);
        assertThat(plan.planName()).isEqualTo("Morning Routine");
        assertThat(plan.entries()).hasSize(1);

        SupplementTodayDto.TodayEntryDto entry = plan.entries().get(0);
        assertThat(entry.supplementName()).isEqualTo("Magnesium");
        assertThat(entry.takenToday()).isTrue();
        assertThat(entry.logId()).isNotNull();
        assertThat(entry.loggedAt()).isNotNull();
    }

    // ── T5: entry not yet logged today → takenToday = false ─────────────────

    @Test
    void whenNotLoggedToday_thenTakenTodayIsFalse() {
        List<SupplementTodayDto> result = logService.getTodayStatus(testUser.getId());

        assertThat(result).hasSize(1);
        SupplementTodayDto.TodayEntryDto entry = result.get(0).entries().get(0);
        assertThat(entry.takenToday()).isFalse();
        assertThat(entry.logId()).isNull();
        assertThat(entry.loggedAt()).isNull();
    }

    // ── T6: inactive plan → entries not included ─────────────────────────────

    @Test
    void whenPlanIsInactive_thenEntriesAreExcluded() {
        SupplementPlan inactivePlan = planRepository.save(SupplementPlan.builder()
                .user(testUser)
                .name("Inactive Plan")
                .active(false)
                .build());

        entryRepository.save(SupplementPlanEntry.builder()
                .plan(inactivePlan)
                .supplement(supplement)
                .timeSlot(TimeSlot.EVENING)
                .build());

        List<SupplementTodayDto> result = logService.getTodayStatus(testUser.getId());

        // Only the active plan's entries are returned
        assertThat(result).hasSize(1);
        assertThat(result.get(0).planName()).isEqualTo("Morning Routine");
    }

    // ── T7: log from yesterday → takenToday = false ──────────────────────────

    @Test
    void whenLoggedYesterday_thenTakenTodayIsFalse() {
        OffsetDateTime yesterday = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        logRepository.save(SupplementLog.builder()
                .user(testUser)
                .supplement(supplement)
                .planEntry(planEntry)
                .takenAt(yesterday)
                .source(SupplementSource.PLANNED)
                .build());

        List<SupplementTodayDto> result = logService.getTodayStatus(testUser.getId());

        assertThat(result).hasSize(1);
        SupplementTodayDto.TodayEntryDto entry = result.get(0).entries().get(0);
        assertThat(entry.takenToday()).isFalse();
        assertThat(entry.logId()).isNull();
    }
}
