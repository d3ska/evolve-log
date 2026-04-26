package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplement_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplement_id", nullable = false)
    private Supplement supplement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_entry_id")
    private SupplementPlanEntry planEntry;

    @Column(name = "taken_at", nullable = false)
    @Builder.Default
    private OffsetDateTime takenAt = OffsetDateTime.now();

    @Column(name = "dose_amount", precision = 10, scale = 3)
    private BigDecimal doseAmount;

    @Column(name = "dose_unit", length = 50)
    private String doseUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SupplementSource source = SupplementSource.SPONTANEOUS;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
