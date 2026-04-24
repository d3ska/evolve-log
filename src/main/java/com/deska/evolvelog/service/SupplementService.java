package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.*;
import com.deska.evolvelog.dto.request.*;
import com.deska.evolvelog.dto.response.*;
import com.deska.evolvelog.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SupplementService {

    private final SupplementRepository supplementRepository;
    private final SupplementPlanRepository planRepository;
    private final SupplementPlanEntryRepository entryRepository;
    private final SupplementLogRepository logRepository;

    public SupplementService(SupplementRepository supplementRepository,
                             SupplementPlanRepository planRepository,
                             SupplementPlanEntryRepository entryRepository,
                             SupplementLogRepository logRepository) {
        this.supplementRepository = supplementRepository;
        this.planRepository = planRepository;
        this.entryRepository = entryRepository;
        this.logRepository = logRepository;
    }

    // ── Supplement catalog ──────────────────────────────────────────────────

    @Transactional
    public SupplementDto createSupplement(User user, CreateSupplementRequest req) {
        Supplement supplement = Supplement.builder()
                .user(user)
                .name(req.name())
                .brand(req.brand())
                .form(req.form())
                .notes(req.notes())
                .build();
        return SupplementDto.from(supplementRepository.save(supplement));
    }

    @Transactional(readOnly = true)
    public List<SupplementDto> listSupplements(UUID userId) {
        return supplementRepository.findByUserIdOrderByNameAsc(userId)
                .stream().map(SupplementDto::from).toList();
    }

    @Transactional
    public void deleteSupplement(UUID id, UUID userId) {
        Supplement s = supplementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplement not found"));
        supplementRepository.delete(s);
    }

    // ── Plans ───────────────────────────────────────────────────────────────

    @Transactional
    public SupplementPlanDto createPlan(User user, CreateSupplementPlanRequest req) {
        SupplementPlan plan = SupplementPlan.builder()
                .user(user)
                .name(req.name())
                .description(req.description())
                .build();
        return SupplementPlanDto.from(planRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<SupplementPlanDto> listPlans(UUID userId) {
        return planRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(SupplementPlanDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SupplementPlanDto getPlan(UUID planId, UUID userId) {
        return SupplementPlanDto.from(requirePlan(planId, userId));
    }

    @Transactional
    public void deletePlan(UUID planId, UUID userId) {
        planRepository.delete(requirePlan(planId, userId));
    }

    // ── Plan entries ────────────────────────────────────────────────────────

    @Transactional
    public SupplementPlanDto addEntry(UUID planId, UUID userId, CreateSupplementPlanEntryRequest req) {
        SupplementPlan plan = requirePlan(planId, userId);
        Supplement supplement = supplementRepository.findByIdAndUserId(req.supplementId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplement not found"));

        SupplementPlanEntry entry = SupplementPlanEntry.builder()
                .plan(plan)
                .supplement(supplement)
                .timeSlot(req.timeSlot())
                .customTime(req.customTime())
                .doseAmount(req.doseAmount())
                .doseUnit(req.doseUnit())
                .notes(req.notes())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build();

        plan.getEntries().add(entry);
        return SupplementPlanDto.from(planRepository.save(plan));
    }

    @Transactional
    public void removeEntry(UUID planId, UUID entryId, UUID userId) {
        SupplementPlan plan = requirePlan(planId, userId);
        plan.getEntries().removeIf(e -> e.getId().equals(entryId));
        planRepository.save(plan);
    }

    // ── Intake log ──────────────────────────────────────────────────────────

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

    @Transactional
    public void deleteLog(UUID logId, UUID userId) {
        SupplementLog log = logRepository.findByIdAndUserId(logId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log entry not found"));
        logRepository.delete(log);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private SupplementPlan requirePlan(UUID planId, UUID userId) {
        return planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }
}
