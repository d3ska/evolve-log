package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_sets")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    private Integer reps;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void update(Integer reps, BigDecimal weightKg, Boolean completed) {
        if (reps != null) this.reps = reps;
        if (weightKg != null) this.weightKg = weightKg;
        if (completed != null) {
            this.completed = completed;
            if (completed && this.completedAt == null) {
                this.completedAt = Instant.now();
            }
            // When unchecking, preserve completedAt so the rest timer continues from the original stamp
        }
    }
}
