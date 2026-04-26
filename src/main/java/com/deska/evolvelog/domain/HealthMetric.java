package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_metrics")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "metric_key", nullable = false, length = 100)
    private String metricKey;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(length = 20)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
