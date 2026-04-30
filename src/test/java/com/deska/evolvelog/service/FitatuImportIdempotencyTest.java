package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.repository.FitatuFoodLogRepository;
import com.deska.evolvelog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FitatuImportIdempotencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    FitatuImportService fitatuImportService;

    @Autowired
    FitatuFoodLogRepository foodLogRepository;

    @Autowired
    UserRepository userRepository;

    private User testUser;

    /**
     * Minimal Fitatu CSV with two food rows on the same date.
     * Headers use the Polish column names that FitatuCsvParser expects.
     */
    private static final String CSV_V1 = """
            Data,Posiłek,Produkt,Ilość (g),kalorie (kcal),Białka (g),Tłuszcze (g),Węglowodany (g)
            2026-01-10,Śniadanie,Owsianka,100,350,12,6,60
            2026-01-10,Obiad,Kurczak,200,330,50,8,0
            """;

    /** Same foods, corrected quantities/calories. */
    private static final String CSV_V2 = """
            Data,Posiłek,Produkt,Ilość (g),kalorie (kcal),Białka (g),Tłuszcze (g),Węglowodany (g)
            2026-01-10,Śniadanie,Owsianka,120,420,14,7,72
            2026-01-10,Obiad,Kurczak,250,413,62,10,0
            """;

    @BeforeEach
    void setUp() {
        foodLogRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test-fitatu@example.com")
                .build());
    }

    // ── T5: re-import same dates → rows updated, no duplicates ───────────────

    @Test
    void reimportSameDates_shouldUpdateRowsWithoutDuplicates() throws IOException {
        fitatuImportService.importCsv(testUser, stream(CSV_V1));
        fitatuImportService.importCsv(testUser, stream(CSV_V2));

        List<?> rows = foodLogRepository.findAll();
        assertThat(rows).hasSize(2);
    }

    @Test
    void reimportSameDates_shouldUpdateQuantityValues() throws IOException {
        fitatuImportService.importCsv(testUser, stream(CSV_V1));
        fitatuImportService.importCsv(testUser, stream(CSV_V2));

        var logs = foodLogRepository.findByUserIdAndDateBetween(
                testUser.getId(),
                java.time.LocalDate.of(2026, 1, 10),
                java.time.LocalDate.of(2026, 1, 10));

        var owsianka = logs.stream()
                .filter(l -> "Owsianka".equals(l.getFoodName()))
                .findFirst()
                .orElseThrow();
        // CSV_V2 has 120g for Owsianka — should be updated from 100g
        assertThat(owsianka.getQuantityG()).isEqualByComparingTo(new BigDecimal("120"));
    }

    @Test
    void importOnce_thenImportNewFood_shouldAddWithoutTouchingExisting() throws IOException {
        fitatuImportService.importCsv(testUser, stream(CSV_V1));

        String csvWithExtra = CSV_V1 + "2026-01-10,Kolacja,Ryba,150,200,30,8,0\n";
        fitatuImportService.importCsv(testUser, stream(csvWithExtra));

        assertThat(foodLogRepository.count()).isEqualTo(3);
    }

    private static ByteArrayInputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
