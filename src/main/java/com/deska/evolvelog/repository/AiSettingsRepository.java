package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiSettingsRepository extends JpaRepository<AiSettings, UUID> {
}
