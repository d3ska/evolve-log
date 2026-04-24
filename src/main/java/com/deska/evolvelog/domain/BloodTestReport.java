package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blood_test_reports")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodTestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "lab_name", length = 200)
    private String labName;

    @Column(length = 500)
    private String filename;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BloodTestResult> results = new ArrayList<>();
}
