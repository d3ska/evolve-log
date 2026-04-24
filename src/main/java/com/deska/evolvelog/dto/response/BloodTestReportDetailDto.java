package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.BloodTestReport;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BloodTestReportDetailDto(
        UUID id,
        LocalDate date,
        String labName,
        String filename,
        OffsetDateTime uploadedAt,
        List<BloodTestResultDto> results
) {
    public static BloodTestReportDetailDto from(BloodTestReport r) {
        return new BloodTestReportDetailDto(
                r.getId(),
                r.getDate(),
                r.getLabName(),
                r.getFilename(),
                r.getUploadedAt(),
                r.getResults().stream().map(BloodTestResultDto::from).toList()
        );
    }
}
