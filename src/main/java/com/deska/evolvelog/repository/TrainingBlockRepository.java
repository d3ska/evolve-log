package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.TrainingBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingBlockRepository extends JpaRepository<TrainingBlock, UUID> {

    List<TrainingBlock> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<TrainingBlock> findByIdAndUserId(UUID id, UUID userId);
}
