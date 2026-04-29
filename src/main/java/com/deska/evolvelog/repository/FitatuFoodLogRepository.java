package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.FitatuFoodLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FitatuFoodLogRepository extends JpaRepository<FitatuFoodLog, UUID> {

    @Query("SELECT f FROM FitatuFoodLog f WHERE f.user.id = :userId AND f.date BETWEEN :from AND :to ORDER BY f.date DESC, f.meal ASC")
    List<FitatuFoodLog> findByUserIdAndDateBetween(@Param("userId") UUID userId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO fitatu_food_logs (id, user_id, date, meal, food_name, quantity_g, nutrients, imported_at)
            VALUES (gen_random_uuid(), :userId, :date, :meal, :foodName, :quantityG, CAST(:nutrients AS jsonb), :importedAt)
            ON CONFLICT (user_id, date, meal, food_name) DO UPDATE SET
                quantity_g  = EXCLUDED.quantity_g,
                nutrients   = EXCLUDED.nutrients,
                imported_at = EXCLUDED.imported_at
            """)
    void upsert(@Param("userId") UUID userId,
                @Param("date") LocalDate date,
                @Param("meal") String meal,
                @Param("foodName") String foodName,
                @Param("quantityG") BigDecimal quantityG,
                @Param("nutrients") String nutrients,
                @Param("importedAt") OffsetDateTime importedAt);
}
