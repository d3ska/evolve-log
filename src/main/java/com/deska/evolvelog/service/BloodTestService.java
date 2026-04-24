package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.BloodTestReport;
import com.deska.evolvelog.domain.BloodTestResult;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.BloodTestHistoryPointDto;
import com.deska.evolvelog.dto.response.BloodTestReportDetailDto;
import com.deska.evolvelog.dto.response.BloodTestReportSummaryDto;
import com.deska.evolvelog.repository.BloodTestReportRepository;
import com.deska.evolvelog.repository.BloodTestResultRepository;
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

    private final BloodTestCsvParser csvParser;
    private final BloodTestReportRepository reportRepository;
    private final BloodTestResultRepository resultRepository;

    public BloodTestService(BloodTestCsvParser csvParser,
                            BloodTestReportRepository reportRepository,
                            BloodTestResultRepository resultRepository) {
        this.csvParser = csvParser;
        this.reportRepository = reportRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional
    public BloodTestReportSummaryDto upload(User user, MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        BloodTestCsvParser.ParseResult parsed = csvParser.parse(file.getInputStream());

        BloodTestReport report = BloodTestReport.builder()
                .user(user)
                .date(parsed.date())
                .labName("Manual CSV")
                .filename(filename)
                .uploadedAt(OffsetDateTime.now())
                .build();

        BloodTestReport saved = reportRepository.save(report);

        List<BloodTestResult> results = parsed.parameters().stream()
                .map(p -> BloodTestResult.builder()
                        .report(saved)
                        .parameterKey(p.key())
                        .parameterLabel(p.label())
                        .value(p.value())
                        .unit(p.unit())
                        .refLow(p.refLow())
                        .refHigh(p.refHigh())
                        .flag(p.flag())
                        .category(p.category())
                        .build())
                .toList();

        saved.getResults().addAll(results);
        reportRepository.save(saved);

        return new BloodTestReportSummaryDto(
                saved.getId(),
                saved.getDate(),
                saved.getLabName(),
                saved.getFilename(),
                saved.getUploadedAt(),
                results.size()
        );
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
