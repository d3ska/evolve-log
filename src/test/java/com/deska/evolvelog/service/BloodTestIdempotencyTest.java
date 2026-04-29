package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.BloodTestReportSummaryDto;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.BloodTestResultRepository;
import com.deska.evolvelog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ai.encryption.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BloodTestIdempotencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    BloodTestService bloodTestService;

    @Autowired
    BloodTestReportRepository reportRepository;

    @Autowired
    BloodTestResultRepository resultRepository;

    @Autowired
    UserRepository userRepository;

    private User testUser;

    private static final String CSV_V1 = """
            date,key,value,unit,ref_low,ref_high
            2026-01-15,glucose,5.2,mmol/L,3.9,6.1
            2026-01-15,creatinine,80,umol/L,62,115
            """;

    private static final String CSV_V2 = """
            date,key,value,unit,ref_low,ref_high
            2026-01-15,glucose,6.0,mmol/L,3.9,6.1
            2026-01-15,creatinine,85,umol/L,62,115
            """;

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        reportRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test-blood@example.com")
                .build());
    }

    // ── T4: upload same CSV twice → single report, results updated ───────────

    @Test
    void uploadTwice_shouldProduceSingleReportWithUpdatedResults() throws IOException {
        MockMultipartFile fileV1 = csv("blood_v1.csv", CSV_V1);
        MockMultipartFile fileV2 = csv("blood_v2.csv", CSV_V2);

        bloodTestService.upload(testUser, fileV1);
        BloodTestReportSummaryDto second = bloodTestService.upload(testUser, fileV2);

        assertThat(reportRepository.count()).isEqualTo(1);
        assertThat(second.resultCount()).isEqualTo(2);

        // Glucose should be updated to 6.0 from the second upload
        var results = resultRepository.findAll();
        assertThat(results).hasSize(2);
        var glucose = results.stream()
                .filter(r -> r.getParameterKey().equals("glucose"))
                .findFirst()
                .orElseThrow();
        assertThat(glucose.getValue()).isEqualByComparingTo("6.0");
    }

    // ── T6: null lab_name normalized to '' — two uploads → single row ────────

    @Test
    void uploadTwiceWithNullLabName_shouldProduceSingleReport() throws IOException {
        MockMultipartFile fileV1 = csv("blood_null_lab_v1.csv", CSV_V1);
        MockMultipartFile fileV2 = csv("blood_null_lab_v2.csv", CSV_V2);

        // null lab_name normalizes to '' in the service
        bloodTestService.uploadWithLabName(testUser, fileV1, null);
        bloodTestService.uploadWithLabName(testUser, fileV2, null);

        assertThat(reportRepository.count()).isEqualTo(1);

        var results = resultRepository.findAll();
        var creatinine = results.stream()
                .filter(r -> r.getParameterKey().equals("creatinine"))
                .findFirst()
                .orElseThrow();
        // Second upload updated creatinine from 80 to 85
        assertThat(creatinine.getValue()).isEqualByComparingTo("85");
    }

    // ── Different dates → separate reports ──────────────────────────────────

    @Test
    void uploadDifferentDates_shouldProduceTwoReports() throws IOException {
        String csvDate2 = CSV_V1.replace("2026-01-15", "2026-01-20");

        bloodTestService.upload(testUser, csv("blood_d1.csv", CSV_V1));
        bloodTestService.upload(testUser, csv("blood_d2.csv", csvDate2));

        assertThat(reportRepository.count()).isEqualTo(2);
    }

    private static MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes());
    }
}
