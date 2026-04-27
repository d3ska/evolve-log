package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "monthly_exercise_aggregates")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExerciseAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exercise_name", nullable = false, length = 255)
    private String exerciseName;

    @Column(name = "exercise_definition_id")
    private UUID exerciseDefinitionId;

    /** First day of the month, e.g. 2025-01-01 */
    @Column(name = "year_month", nullable = false)
    private LocalDate yearMonth;

    @Column(name = "max_weight_kg", precision = 8, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "total_volume_kg", precision = 12, scale = 2)
    private BigDecimal totalVolumeKg;

    @Column(name = "session_count", nullable = false)
    @Builder.Default
    private int sessionCount = 0;

    @Column(name = "total_sets", nullable = false)
    @Builder.Default
    private int totalSets = 0;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @PrePersist
    protected void onCreate() {
        if (computedAt == null) {
            computedAt = OffsetDateTime.now();
        }
    }
}
