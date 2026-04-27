package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.AiInsight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiInsightRepository extends JpaRepository<AiInsight, UUID> {

    Page<AiInsight> findByUserIdOrderByGeneratedAtDesc(UUID userId, Pageable pageable);

    Page<AiInsight> findByUserIdAndTypeOrderByGeneratedAtDesc(UUID userId, String type, Pageable pageable);
}
