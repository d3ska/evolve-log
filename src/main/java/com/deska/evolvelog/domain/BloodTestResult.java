package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "blood_test_results")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private BloodTestReport report;

    @Column(name = "parameter_key", nullable = false, length = 100)
    private String parameterKey;

    @Column(name = "parameter_label", nullable = false, length = 200)
    private String parameterLabel;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(length = 50)
    private String unit;

    @Column(name = "ref_low", precision = 12, scale = 4)
    private BigDecimal refLow;

    @Column(name = "ref_high", precision = 12, scale = 4)
    private BigDecimal refHigh;

    @Column(length = 10)
    private String flag;

    @Column(length = 100)
    private String category;
}
