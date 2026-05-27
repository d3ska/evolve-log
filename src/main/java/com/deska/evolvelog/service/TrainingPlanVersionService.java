package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.TrainingPlanVersion;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.TrainingPlanVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingPlanVersionService {

    private final TrainingPlanVersionRepository versionRepository;
    private final TrainingPlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<TrainingPlanVersion> listVersions(UUID planId, UUID userId) {
        assertOwnership(planId, userId);
        return versionRepository.findByTrainingPlanIdOrderByVersionAsc(planId);
    }

    @Transactional(readOnly = true)
    public TrainingPlanVersion getVersion(UUID planId, Integer version, UUID userId) {
        assertOwnership(planId, userId);
        return versionRepository.findByTrainingPlanIdAndVersion(planId, version)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingPlanVersion", planId));
    }

    private void assertOwnership(UUID planId, UUID userId) {
        planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("TrainingPlan", planId));
    }
}
