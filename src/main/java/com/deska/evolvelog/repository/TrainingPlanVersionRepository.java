package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.TrainingPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanVersionRepository extends JpaRepository<TrainingPlanVersion, UUID> {

    List<TrainingPlanVersion> findByTrainingPlanIdOrderByVersionAsc(UUID trainingPlanId);

    Optional<TrainingPlanVersion> findByTrainingPlanIdAndVersion(UUID trainingPlanId, Integer version);
}
