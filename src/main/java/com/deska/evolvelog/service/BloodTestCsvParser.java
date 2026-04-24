package com.deska.evolvelog.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a manually-created blood test CSV file.
 *
 * Expected format (first row = header):
 *   date,key,value,unit,ref_low,ref_high
 *
 * - date      : YYYY-MM-DD (only needs to appear once; all rows share the same date)
 * - key       : parameter key matching ParameterCatalog (e.g. glucose, wbc)
 * - value     : numeric value (dot or comma decimal separator)
 * - unit      : e.g. mmol/L, g/dL, %
 * - ref_low   : lower reference bound (optional, leave empty if unknown)
 * - ref_high  : upper reference bound (optional)
 *
 * Example:
 *   date,key,value,unit,ref_low,ref_high
 *   2026-04-20,glucose,5.2,mmol/L,3.9,6.1
 *   2026-04-20,wbc,6.5,10^3/µL,4.0,10.0
 *   2026-04-20,hemoglobin,14.2,g/dL,13.5,17.5
 */
@Service
public class BloodTestCsvParser {

    private static final Logger log = LoggerFactory.getLogger(BloodTestCsvParser.class);

    public record ParsedParameter(
            String key,
            String label,
            BigDecimal value,
            String unit,
            BigDecimal refLow,
            BigDecimal refHigh,
            String flag,
            String category
    ) {}

    public record ParseResult(LocalDate date, List<ParsedParameter> parameters) {}

    public ParseResult parse(InputStream inputStream) throws IOException {
        List<ParsedParameter> results = new ArrayList<>();
        LocalDate date = LocalDate.now();

        try (CSVParser csv = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            for (CSVRecord record : csv) {
                LocalDate rowDate = parseDate(record.get("date"));
                if (rowDate != null) date = rowDate;

                String rawKey = record.get("key");
                String key = ParameterCatalog.resolveKey(rawKey).orElse(null);
                if (key == null) {
                    log.warn("Unknown parameter key '{}' — skipping", rawKey);
                    continue;
                }

                BigDecimal value = parseDecimal(record.get("value"));
                if (value == null) {
                    log.warn("Skipping row with unparseable value: {}", record);
                    continue;
                }

                String unit     = record.get("unit");
                BigDecimal low  = parseDecimal(safeGet(record, "ref_low"));
                BigDecimal high = parseDecimal(safeGet(record, "ref_high"));

                String flag = deriveFlag(value, low, high);

                ParameterCatalog.ParamMeta meta = ParameterCatalog.find(key).orElseThrow();

                results.add(new ParsedParameter(
                        key, meta.label(), value, unit, low, high, flag, meta.category()));
            }
        }

        log.info("CSV parsed: date={} parameters={}", date, results.size());
        return new ParseResult(date, results);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.replace("<", "").replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeGet(CSVRecord record, String column) {
        try {
            return record.get(column);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String deriveFlag(BigDecimal value, BigDecimal low, BigDecimal high) {
        if (low == null || high == null) return null;
        if (value.compareTo(high) > 0) return "H";
        if (value.compareTo(low)  < 0) return "L";
        return null;
    }
}
