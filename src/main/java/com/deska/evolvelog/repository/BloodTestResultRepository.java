package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.BloodTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BloodTestResultRepository extends JpaRepository<BloodTestResult, UUID> {

    @Query("""
        SELECT r FROM BloodTestResult r
        JOIN FETCH r.report rep
        WHERE rep.user.id = :userId AND r.parameterKey = :key
        ORDER BY rep.date ASC
        """)
    List<BloodTestResult> findHistoryByUserIdAndKey(@Param("userId") UUID userId,
                                                    @Param("key") String key);
}
