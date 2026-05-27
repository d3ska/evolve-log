package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "training_plans")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private TrainingBlock block;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "current_version", nullable = false)
    private int currentVersion = 1;

    @Builder.Default
    @OneToMany(mappedBy = "trainingPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PlannedExercise> plannedExercises = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getBlockId() {
        return block != null ? block.getId() : null;
    }

    public void setBlock(TrainingBlock block) {
        this.block = block;
    }

    public void incrementVersion() {
        this.currentVersion++;
    }

    public void applyPatch(String name, String description, DayOfWeek dayOfWeek, Boolean isActive) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (dayOfWeek != null) this.dayOfWeek = dayOfWeek;
        if (isActive != null) this.isActive = isActive;
    }
}
