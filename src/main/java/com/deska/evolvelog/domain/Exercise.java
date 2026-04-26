package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exercises")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer sets;

    private Integer reps;

    @Column(precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "exercise_definition_id")
    private UUID exerciseDefinitionId;

    @Column(precision = 3, scale = 1)
    private BigDecimal rpe;

    @Column(name = "primary_muscle", length = 50)
    private String primaryMuscle;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void applyPatch(String name, Integer sets, Integer reps, BigDecimal weightKg, String notes,
                           Integer position, UUID exerciseDefinitionId, BigDecimal rpe, String primaryMuscle) {
        if (name != null) this.name = name;
        if (sets != null) this.sets = sets;
        if (reps != null) this.reps = reps;
        if (weightKg != null) this.weightKg = weightKg;
        if (notes != null) this.notes = notes;
        if (position != null) this.position = position;
        if (exerciseDefinitionId != null) this.exerciseDefinitionId = exerciseDefinitionId;
        if (rpe != null) this.rpe = rpe;
        if (primaryMuscle != null) this.primaryMuscle = primaryMuscle;
    }
}
