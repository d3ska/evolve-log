package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.*;
import com.deska.evolvelog.dto.request.CreateSupplementPlanEntryRequest;
import com.deska.evolvelog.dto.request.CreateSupplementPlanRequest;
import com.deska.evolvelog.dto.response.SupplementPlanDto;
import com.deska.evolvelog.repository.SupplementPlanEntryRepository;
import com.deska.evolvelog.repository.SupplementPlanRepository;
import com.deska.evolvelog.repository.SupplementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SupplementPlanService {

    private final SupplementPlanRepository planRepository;
    private final SupplementPlanEntryRepository entryRepository;
    private final SupplementRepository supplementRepository;

    public SupplementPlanService(SupplementPlanRepository planRepository,
                                 SupplementPlanEntryRepository entryRepository,
                                 SupplementRepository supplementRepository) {
        this.planRepository = planRepository;
        this.entryRepository = entryRepository;
        this.supplementRepository = supplementRepository;
    }

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

    private SupplementPlan requirePlan(UUID planId, UUID userId) {
        return planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }
}
