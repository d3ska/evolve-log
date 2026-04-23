package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "measurements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(precision = 4, scale = 1)
    private BigDecimal bodyFatPercent;

    @Column(precision = 5, scale = 1)
    private BigDecimal chestCm;

    @Column(name = "waist_narrowest_cm", precision = 5, scale = 1)
    private BigDecimal waistNarrowestCm;

    @Column(name = "waist_navel_cm", precision = 5, scale = 1)
    private BigDecimal waistNavelCm;

    @Column(name = "biceps_cm", precision = 5, scale = 1)
    private BigDecimal bicepsCm;

    @Column(name = "thigh_cm", precision = 5, scale = 1)
    private BigDecimal thighCm;

    @Column(name = "calves_cm", precision = 5, scale = 1)
    private BigDecimal calvesCm;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
