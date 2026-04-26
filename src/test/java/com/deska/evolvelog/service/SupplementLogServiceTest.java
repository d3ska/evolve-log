package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.*;
import com.deska.evolvelog.dto.request.LogSupplementRequest;
import com.deska.evolvelog.dto.response.SupplementLogDto;
import com.deska.evolvelog.repository.SupplementLogRepository;
import com.deska.evolvelog.repository.SupplementPlanEntryRepository;
import com.deska.evolvelog.repository.SupplementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementLogServiceTest {

    @Mock
    SupplementLogRepository logRepository;

    @Mock
    SupplementRepository supplementRepository;

    @Mock
    SupplementPlanEntryRepository entryRepository;

    @InjectMocks
    SupplementLogService service;

    private User user;
    private Supplement supplement;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        supplement = Supplement.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Magnesium")
                .build();
    }

    @Test
    void shouldLogIntakeAsSpontaneousWhenNoPlanEntryProvided() {
        // given
        var req = new LogSupplementRequest(supplement.getId(), null, null, null, null, null);
        var savedLog = SupplementLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .supplement(supplement)
                .source(SupplementSource.SPONTANEOUS)
                .build();
        when(supplementRepository.findByIdAndUserId(supplement.getId(), user.getId()))
                .thenReturn(Optional.of(supplement));
        when(logRepository.save(any())).thenReturn(savedLog);

        // when
        SupplementLogDto result = service.logIntake(user, req);

        // then
        assertThat(result.source()).isEqualTo(SupplementSource.SPONTANEOUS);
        verify(logRepository).save(any(SupplementLog.class));
    }

    @Test
    void shouldLogIntakeAsPlannedWhenPlanEntryIdProvided() {
        // given
        UUID planEntryId = UUID.randomUUID();
        var planEntry = SupplementPlanEntry.builder()
                .id(planEntryId)
                .supplement(supplement)
                .timeSlot(TimeSlot.MORNING)
                .build();
        var req = new LogSupplementRequest(supplement.getId(), planEntryId, null, null, null, null);
        var savedLog = SupplementLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .supplement(supplement)
                .planEntry(planEntry)
                .source(SupplementSource.PLANNED)
                .build();
        when(supplementRepository.findByIdAndUserId(supplement.getId(), user.getId()))
                .thenReturn(Optional.of(supplement));
        when(entryRepository.findByIdAndPlanUserId(planEntryId, user.getId()))
                .thenReturn(Optional.of(planEntry));
        when(logRepository.save(any())).thenReturn(savedLog);

        // when
        SupplementLogDto result = service.logIntake(user, req);

        // then
        assertThat(result.source()).isEqualTo(SupplementSource.PLANNED);
        assertThat(result.planEntryId()).isEqualTo(planEntryId);
    }

    @Test
    void shouldReturnLogsForDateWhenListingByDate() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 15);
        OffsetDateTime start = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        var log = SupplementLog.builder()
                .id(UUID.randomUUID())
                .user(user)
                .supplement(supplement)
                .source(SupplementSource.SPONTANEOUS)
                .build();
        when(logRepository.findByUserIdAndTakenAtBetweenOrderByTakenAtDesc(
                eq(user.getId()), eq(start), eq(end)))
                .thenReturn(List.of(log));

        // when
        List<SupplementLogDto> result = service.listLogsByDate(user.getId(), date);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldCallDeleteWhenDeletingExistingLog() {
        // given
        UUID logId = UUID.randomUUID();
        var log = SupplementLog.builder()
                .id(logId)
                .user(user)
                .supplement(supplement)
                .source(SupplementSource.SPONTANEOUS)
                .build();
        when(logRepository.findByIdAndUserId(logId, user.getId())).thenReturn(Optional.of(log));

        // when
        service.deleteLog(logId, user.getId());

        // then
        verify(logRepository).delete(log);
    }

    @Test
    void shouldThrow404WhenDeletingLogWithUnknownId() {
        // given
        UUID logId = UUID.randomUUID();
        when(logRepository.findByIdAndUserId(logId, user.getId())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.deleteLog(logId, user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
