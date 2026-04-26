package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercise_definitions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "primary_muscle", nullable = false, length = 50)
    private String primaryMuscle;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 20)
    @Column(name = "secondary_muscles", columnDefinition = "text[]")
    private List<String> secondaryMuscles;

    @Column(length = 50)
    private String equipment;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
