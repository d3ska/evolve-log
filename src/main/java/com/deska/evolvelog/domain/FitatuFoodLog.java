package com.deska.evolvelog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fitatu_food_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FitatuFoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 100)
    private String meal;

    @Column(name = "food_name", length = 500)
    private String foodName;

    @Column(name = "quantity_g", precision = 10, scale = 2)
    private BigDecimal quantityG;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, BigDecimal> nutrients;

    @Column(name = "imported_at", nullable = false)
    private OffsetDateTime importedAt;
}
