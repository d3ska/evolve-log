package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "withings_measurements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithingsMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "body_fat_percent", precision = 4, scale = 1)
    private BigDecimal bodyFatPercent;

    @Column(name = "muscle_mass_kg", precision = 5, scale = 2)
    private BigDecimal muscleMassKg;

    @Column(name = "bone_mass_kg", precision = 4, scale = 2)
    private BigDecimal boneMassKg;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
