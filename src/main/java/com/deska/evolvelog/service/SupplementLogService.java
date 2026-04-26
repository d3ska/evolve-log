package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.*;
import com.deska.evolvelog.dto.request.LogSupplementRequest;
import com.deska.evolvelog.dto.response.SupplementLogDto;
import com.deska.evolvelog.repository.SupplementLogRepository;
import com.deska.evolvelog.repository.SupplementPlanEntryRepository;
import com.deska.evolvelog.repository.SupplementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class SupplementLogService {

    private final SupplementLogRepository logRepository;
    private final SupplementRepository supplementRepository;
    private final SupplementPlanEntryRepository entryRepository;

    public SupplementLogService(SupplementLogRepository logRepository,
                                SupplementRepository supplementRepository,
                                SupplementPlanEntryRepository entryRepository) {
        this.logRepository = logRepository;
        this.supplementRepository = supplementRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public SupplementLogDto logIntake(User user, LogSupplementRequest req) {
        Supplement supplement = supplementRepository.findByIdAndUserId(req.supplementId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplement not found"));

        SupplementPlanEntry planEntry = null;
        SupplementSource source = SupplementSource.SPONTANEOUS;
        if (req.planEntryId() != null) {
            planEntry = entryRepository.findByIdAndPlanUserId(req.planEntryId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan entry not found"));
            source = SupplementSource.PLANNED;
        }

        SupplementLog log = SupplementLog.builder()
                .user(user)
                .supplement(supplement)
                .planEntry(planEntry)
                .takenAt(req.takenAt() != null ? req.takenAt() : OffsetDateTime.now())
                .doseAmount(req.doseAmount())
                .doseUnit(req.doseUnit())
                .source(source)
                .notes(req.notes())
                .build();

        return SupplementLogDto.from(logRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<SupplementLogDto> listLogs(UUID userId, int limit) {
        return logRepository.findByUserIdOrderByTakenAtDesc(userId, PageRequest.of(0, limit))
                .stream().map(SupplementLogDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SupplementLogDto> listLogsByDate(UUID userId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        return logRepository.findByUserIdAndTakenAtBetweenOrderByTakenAtDesc(userId, start, end)
                .stream().map(SupplementLogDto::from).toList();
    }

    @Transactional
    public void deleteLog(UUID logId, UUID userId) {
        SupplementLog log = logRepository.findByIdAndUserId(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        logRepository.delete(log);
    }
}
