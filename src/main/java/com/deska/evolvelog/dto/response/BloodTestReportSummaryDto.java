package com.deska.evolvelog.dto.response;

import com.deska.evolvelog.domain.BloodTestReport;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BloodTestReportSummaryDto(
        UUID id,
        LocalDate date,
        String labName,
        String filename,
        OffsetDateTime uploadedAt,
        int resultCount
) {
    public static BloodTestReportSummaryDto from(BloodTestReport r) {
        return new BloodTestReportSummaryDto(
                r.getId(),
                r.getDate(),
                r.getLabName(),
                r.getFilename(),
                r.getUploadedAt(),
                r.getResults().size()
        );
    }
}
