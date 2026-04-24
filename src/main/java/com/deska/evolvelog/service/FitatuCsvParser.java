package com.deska.evolvelog.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FitatuCsvParser {

    private static final Logger log = LoggerFactory.getLogger(FitatuCsvParser.class);

    // Maps Polish CSV header → metric key stored in health_metrics
    private static final Map<String, String> COLUMN_MAP = Map.ofEntries(
            Map.entry("kalorie (kcal)",          "kcal"),
            Map.entry("Białka (g)",              "protein_g"),
            Map.entry("Roślinne (g)",            "protein_plant_g"),
            Map.entry("Zwierzęce (g)",           "protein_animal_g"),
            Map.entry("Tłuszcze (g)",            "fat_g"),
            Map.entry("Nasycone (g)",            "fat_saturated_g"),
            Map.entry("Jednonienasycone (g)",    "fat_mono_g"),
            Map.entry("Wielonienasycone (g)",    "fat_poly_g"),
            Map.entry("Kwas omega 3 (g)",        "omega3_g"),
            Map.entry("Kwas omega 6 (g)",        "omega6_g"),
            Map.entry("Węglowodany (g)",         "carbs_g"),
            Map.entry("Cukry (g)",               "sugar_g"),
            Map.entry("Cholesterol (mg)",        "cholesterol_mg"),
            Map.entry("Błonnik (g)",             "fiber_g"),
            Map.entry("Kofeina (mg)",            "caffeine_mg"),
            Map.entry("Kwas foliowy (ug)",       "folate_ug"),
            Map.entry("Witamina A (ug)",         "vitamin_a_ug"),
            Map.entry("Witamina B1 (mg)",        "vitamin_b1_mg"),
            Map.entry("Witamina B2 (mg)",        "vitamin_b2_mg"),
            Map.entry("Witamina B5 (mg)",        "vitamin_b5_mg"),
            Map.entry("Witamina B6 (mg)",        "vitamin_b6_mg"),
            Map.entry("Biotyna (ug)",            "biotin_ug"),
            Map.entry("Witamina B12 (ug)",       "vitamin_b12_ug"),
            Map.entry("Witamina C (mg)",         "vitamin_c_mg"),
            Map.entry("Witamina D (ug)",         "vitamin_d_ug"),
            Map.entry("Witamina E (mg)",         "vitamin_e_mg"),
            Map.entry("Witamina PP (mg)",        "niacin_mg"),
            Map.entry("Witamina K (ug)",         "vitamin_k_ug"),
            Map.entry("Cynk (mg)",               "zinc_mg"),
            Map.entry("Fosfor (mg)",             "phosphorus_mg"),
            Map.entry("Jod (ug)",                "iodine_ug"),
            Map.entry("Magnez (mg)",             "magnesium_mg"),
            Map.entry("Miedź (mg)",              "copper_mg"),
            Map.entry("Potas (mg)",              "potassium_mg"),
            Map.entry("Selen (ug)",              "selenium_ug"),
            Map.entry("Sód (mg)",                "sodium_mg"),
            Map.entry("Wapń (mg)",               "calcium_mg"),
            Map.entry("Żelazo (mg)",             "iron_mg"),
            Map.entry("Sól (g)",                 "salt_g")
    );

    /**
     * Parses a Fitatu CSV export and aggregates all nutritional values by date.
     *
     * @param inputStream the CSV file stream
     * @return map of date → (metric key → summed daily value)
     */
    public Map<LocalDate, Map<String, BigDecimal>> parse(InputStream inputStream) throws IOException {
        Map<LocalDate, Map<String, BigDecimal>> result = new LinkedHashMap<>();

        // Read all bytes and strip UTF-8 BOM (common in Polish Windows exports: EF BB BF)
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(new StringReader(content))) {

            for (CSVRecord record : parser) {
                if (!record.isMapped("Data")) continue;
                String dateStr = record.get("Data");
                if (dateStr == null || dateStr.isBlank()) continue;

                LocalDate date;
                try {
                    date = LocalDate.parse(dateStr.trim());
                } catch (Exception e) {
                    log.warn("Skipping row with unparseable date: {}", dateStr);
                    continue;
                }

                Map<String, BigDecimal> dayTotals = result.computeIfAbsent(date, k -> new LinkedHashMap<>());

                for (Map.Entry<String, String> mapping : COLUMN_MAP.entrySet()) {
                    String csvHeader = mapping.getKey();
                    String metricKey = mapping.getValue();

                    if (!record.isMapped(csvHeader)) continue;

                    String raw = record.get(csvHeader);
                    if (raw == null || raw.isBlank()) continue;

                    try {
                        BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
                        dayTotals.merge(metricKey, value, BigDecimal::add);
                    } catch (NumberFormatException e) {
                        log.debug("Non-numeric value '{}' for column '{}', skipping", raw, csvHeader);
                    }
                }
            }
        }

        return result;
    }
}
