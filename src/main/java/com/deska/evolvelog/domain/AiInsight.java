package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_insights")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model_used", nullable = false, length = 100)
    private String modelUsed;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = OffsetDateTime.now();
        }
    }
}
