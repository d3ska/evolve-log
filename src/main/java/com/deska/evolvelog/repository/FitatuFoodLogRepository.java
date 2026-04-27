package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.FitatuFoodLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FitatuFoodLogRepository extends JpaRepository<FitatuFoodLog, UUID> {

    @Query("SELECT f FROM FitatuFoodLog f WHERE f.user.id = :userId AND f.date BETWEEN :from AND :to ORDER BY f.date DESC, f.meal ASC")
    List<FitatuFoodLog> findByUserIdAndDateBetween(@Param("userId") UUID userId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Modifying
    @Query("DELETE FROM FitatuFoodLog f WHERE f.user.id = :userId AND f.date IN :dates")
    void deleteByUserIdAndDateIn(@Param("userId") UUID userId, @Param("dates") List<LocalDate> dates);
}
