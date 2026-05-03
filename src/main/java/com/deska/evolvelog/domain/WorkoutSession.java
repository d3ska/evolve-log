package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id")
    private TrainingPlan trainingPlan;

    @Column(nullable = false)
    private LocalDateTime date;

    private Integer durationMinutes;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutSessionStatus status = WorkoutSessionStatus.MANUAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 20)
    private List<Exercise> exercises = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void applyPatch(LocalDateTime date, Integer durationMinutes, String notes, TrainingPlan trainingPlan) {
        if (date != null) this.date = date;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
        if (notes != null) this.notes = notes;
        if (trainingPlan != null) this.trainingPlan = trainingPlan;
    }

    public void finish(LocalDateTime finishedAt, Integer durationMinutes) {
        this.finishedAt = finishedAt;
        this.status = WorkoutSessionStatus.FINISHED;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
    }

    public void activate() {
        this.status = WorkoutSessionStatus.ACTIVE;
    }
}
