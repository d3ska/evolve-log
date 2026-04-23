package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "planned_exercises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer sets;

    @Column(nullable = false)
    private Integer repsMin;

    @Column(nullable = false)
    private Integer repsMax;

    private Integer restSeconds;

    @Column(nullable = false)
    private Integer position;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
