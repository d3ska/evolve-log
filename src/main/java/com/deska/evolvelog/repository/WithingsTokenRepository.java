package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.WithingsToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WithingsTokenRepository extends JpaRepository<WithingsToken, UUID> {

    Optional<WithingsToken> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
