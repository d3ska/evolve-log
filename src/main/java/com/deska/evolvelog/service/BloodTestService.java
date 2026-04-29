package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.BloodTestReport;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.BloodTestHistoryPointDto;
import com.deska.evolvelog.dto.response.BloodTestReportDetailDto;
import com.deska.evolvelog.dto.response.BloodTestReportSummaryDto;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.BloodTestResultRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BloodTestService {

    private static final String DEFAULT_LAB_NAME = "Manual CSV";

    private final BloodTestCsvParser csvParser;
    private final BloodTestReportRepository reportRepository;
    private final BloodTestResultRepository resultRepository;
    private final EntityManager entityManager;

    public BloodTestService(BloodTestCsvParser csvParser,
                            BloodTestReportRepository reportRepository,
                            BloodTestResultRepository resultRepository,
                            EntityManager entityManager) {
        this.csvParser = csvParser;
        this.reportRepository = reportRepository;
        this.resultRepository = resultRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public BloodTestReportSummaryDto upload(User user, MultipartFile file) throws IOException {
        return uploadWithLabName(user, file, DEFAULT_LAB_NAME);
    }

    /** Package-private — allows integration tests to vary the lab name (e.g. to test null→'' normalization). */
    @Transactional
    BloodTestReportSummaryDto uploadWithLabName(User user, MultipartFile file, String labName) throws IOException {
        String filename = file.getOriginalFilename();
        BloodTestCsvParser.ParseResult parsed = csvParser.parse(file.getInputStream());
        String normalizedLabName = labName != null ? labName : "";
        OffsetDateTime now = OffsetDateTime.now();

        UUID reportId = upsertReport(user.getId(), parsed.date(), normalizedLabName, filename, now);

        for (BloodTestCsvParser.ParsedParameter p : parsed.parameters()) {
            upsertResult(reportId, p);
        }

        return new BloodTestReportSummaryDto(
                reportId,
                parsed.date(),
                normalizedLabName,
                filename,
                now,
                parsed.parameters().size()
        );
    }

    @SuppressWarnings("unchecked")
    private UUID upsertReport(UUID userId, java.time.LocalDate date, String labName,
                              String filename, OffsetDateTime uploadedAt) {
        List<UUID> inserted = entityManager.createNativeQuery("""
                INSERT INTO blood_test_reports (id, user_id, date, lab_name, filename, uploaded_at)
                VALUES (gen_random_uuid(), :userId, :date, :labName, :filename, :uploadedAt)
                ON CONFLICT (user_id, date, lab_name) DO NOTHING
                RETURNING id
                """, UUID.class)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .setParameter("labName", labName)
                .setParameter("filename", filename)
                .setParameter("uploadedAt", uploadedAt)
                .getResultList();

        if (!inserted.isEmpty()) {
            return inserted.get(0);
        }

        return reportRepository.findIdByUserIdAndDateAndLabName(userId, date, labName);
    }

    private void upsertResult(UUID reportId, BloodTestCsvParser.ParsedParameter p) {
        entityManager.createNativeQuery("""
                INSERT INTO blood_test_results
                    (id, report_id, parameter_key, parameter_label, value, unit, ref_low, ref_high, flag, category)
                VALUES
                    (gen_random_uuid(), :reportId, :key, :label, :value, :unit, :refLow, :refHigh, :flag, :category)
                ON CONFLICT (report_id, parameter_key) DO UPDATE SET
                    parameter_label = EXCLUDED.parameter_label,
                    value           = EXCLUDED.value,
                    unit            = EXCLUDED.unit,
                    ref_low         = EXCLUDED.ref_low,
                    ref_high        = EXCLUDED.ref_high,
                    flag            = EXCLUDED.flag,
                    category        = EXCLUDED.category
                """)
                .setParameter("reportId", reportId)
                .setParameter("key", p.key())
                .setParameter("label", p.label())
                .setParameter("value", p.value())
                .setParameter("unit", p.unit())
                .setParameter("refLow", p.refLow())
                .setParameter("refHigh", p.refHigh())
                .setParameter("flag", p.flag())
                .setParameter("category", p.category())
                .executeUpdate();
    }

    @Transactional(readOnly = true)
    public List<BloodTestReportSummaryDto> findAll(UUID userId) {
        return reportRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(BloodTestReportSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BloodTestReportDetailDto findById(UUID id, UUID userId) {
        BloodTestReport report = reportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        return BloodTestReportDetailDto.from(report);
    }

    @Transactional(readOnly = true)
    public List<BloodTestHistoryPointDto> findHistory(UUID userId, String key) {
        return resultRepository.findHistoryByUserIdAndKey(userId, key)
                .stream()
                .map(BloodTestHistoryPointDto::from)
                .toList();
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        BloodTestReport report = reportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        reportRepository.delete(report);
    }
}
