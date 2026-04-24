package com.deska.evolvelog.service;

import java.util.Map;
import java.util.Optional;

/**
 * Single source of truth for blood-test parameter metadata (label + category).
 * Used by both DiagnostykaPdfParser and BloodTestCsvParser.
 */
public final class ParameterCatalog {

    public record ParamMeta(String label, String category) {}

    private static final Map<String, ParamMeta> CATALOG = Map.ofEntries(
            // Coagulation
            Map.entry("prothrombin_time",    new ParamMeta("Prothrombin Time",    "Coagulation")),
            Map.entry("prothrombin_index",   new ParamMeta("Prothrombin Index",   "Coagulation")),
            Map.entry("inr",                 new ParamMeta("INR",                 "Coagulation")),
            Map.entry("aptt",                new ParamMeta("APTT",                "Coagulation")),
            Map.entry("fibrinogen",          new ParamMeta("Fibrinogen",          "Coagulation")),
            // Kidney
            Map.entry("urea",                new ParamMeta("Urea",                "Kidney")),
            Map.entry("bun",                 new ParamMeta("BUN",                 "Kidney")),
            Map.entry("creatinine",          new ParamMeta("Creatinine",          "Kidney")),
            Map.entry("egfr",                new ParamMeta("eGFR",                "Kidney")),
            // Metabolic
            Map.entry("uric_acid",           new ParamMeta("Uric Acid",           "Metabolic")),
            Map.entry("glucose",             new ParamMeta("Glucose",             "Metabolic")),
            Map.entry("homocysteine",        new ParamMeta("Homocysteine",        "Metabolic")),
            Map.entry("hba1c",               new ParamMeta("HbA1c",               "Metabolic")),
            // Lipids
            Map.entry("total_cholesterol",   new ParamMeta("Total Cholesterol",   "Lipids")),
            Map.entry("hdl_cholesterol",     new ParamMeta("HDL Cholesterol",     "Lipids")),
            Map.entry("non_hdl_cholesterol", new ParamMeta("Non-HDL Cholesterol", "Lipids")),
            Map.entry("ldl_cholesterol",     new ParamMeta("LDL Cholesterol",     "Lipids")),
            Map.entry("triglycerides",       new ParamMeta("Triglycerides",       "Lipids")),
            // Liver
            Map.entry("alp",                 new ParamMeta("ALP",                 "Liver")),
            Map.entry("total_bilirubin",     new ParamMeta("Total Bilirubin",     "Liver")),
            Map.entry("alt",                 new ParamMeta("ALT",                 "Liver")),
            Map.entry("ast",                 new ParamMeta("AST",                 "Liver")),
            Map.entry("ggtp",                new ParamMeta("GGTP",                "Liver")),
            Map.entry("amylase",             new ParamMeta("Amylase",             "Liver")),
            // Electrolytes
            Map.entry("potassium",           new ParamMeta("Potassium",           "Electrolytes")),
            Map.entry("sodium",              new ParamMeta("Sodium",              "Electrolytes")),
            Map.entry("total_calcium",       new ParamMeta("Total Calcium",       "Electrolytes")),
            Map.entry("magnesium",           new ParamMeta("Magnesium",           "Electrolytes")),
            Map.entry("iron",                new ParamMeta("Iron",                "Electrolytes")),
            Map.entry("ferritin",            new ParamMeta("Ferritin",            "Electrolytes")),
            // Inflammation & Thyroid
            Map.entry("crp",                 new ParamMeta("CRP",                 "Inflammation")),
            Map.entry("tsh",                 new ParamMeta("TSH",                 "Thyroid")),
            // CBC
            Map.entry("wbc",                 new ParamMeta("WBC",                 "CBC")),
            Map.entry("rbc",                 new ParamMeta("RBC",                 "CBC")),
            Map.entry("hemoglobin",          new ParamMeta("Hemoglobin",          "CBC")),
            Map.entry("hematocrit",          new ParamMeta("Hematocrit",          "CBC")),
            Map.entry("mcv",                 new ParamMeta("MCV",                 "CBC")),
            Map.entry("mch",                 new ParamMeta("MCH",                 "CBC")),
            Map.entry("mchc",                new ParamMeta("MCHC",                "CBC")),
            Map.entry("platelets",           new ParamMeta("Platelets",           "CBC")),
            Map.entry("neutrophils_pct",     new ParamMeta("Neutrophils %",       "CBC")),
            Map.entry("lymphocytes_pct",     new ParamMeta("Lymphocytes %",       "CBC")),
            Map.entry("monocytes_pct",       new ParamMeta("Monocytes %",         "CBC")),
            Map.entry("eosinophils_pct",     new ParamMeta("Eosinophils %",       "CBC")),
            Map.entry("basophils_pct",       new ParamMeta("Basophils %",         "CBC")),
            // Vitamins
            Map.entry("vitamin_d",           new ParamMeta("Vitamin D",           "Vitamins")),
            Map.entry("vitamin_b12",         new ParamMeta("Vitamin B12",         "Vitamins")),
            Map.entry("folate",              new ParamMeta("Folate",              "Vitamins"))
    );

    // Normalized Polish name → canonical key
    private static final Map<String, String> POLISH_TO_KEY = Map.ofEntries(
            Map.entry("czas protrombinowy",   "prothrombin_time"),
            Map.entry("wskaznik protrombiny", "prothrombin_index"),
            Map.entry("inr",                  "inr"),
            Map.entry("aptt",                 "aptt"),
            Map.entry("fibrynogen",           "fibrinogen"),
            Map.entry("mocznik",              "urea"),
            Map.entry("azot mocznika",        "bun"),
            Map.entry("kreatynina",           "creatinine"),
            Map.entry("egfr",                 "egfr"),
            Map.entry("kwas moczowy",         "uric_acid"),
            Map.entry("glukoza",              "glucose"),
            Map.entry("cholesterol calkowity","total_cholesterol"),
            Map.entry("cholesterol hdl",      "hdl_cholesterol"),
            Map.entry("cholesterol nie-hdl",  "non_hdl_cholesterol"),
            Map.entry("cholesterol ldl",      "ldl_cholesterol"),
            Map.entry("triglicerydy",         "triglycerides"),
            Map.entry("fosfataza zasadowa",   "alp"),
            Map.entry("bilirubina calkowita", "total_bilirubin"),
            Map.entry("alt",                  "alt"),
            Map.entry("ast",                  "ast"),
            Map.entry("ggtp",                 "ggtp"),
            Map.entry("amylaza",              "amylase"),
            Map.entry("potas",                "potassium"),
            Map.entry("sod",                  "sodium"),
            Map.entry("wapn calkowity",       "total_calcium"),
            Map.entry("magnez",               "magnesium"),
            Map.entry("zelazo",               "iron"),
            Map.entry("ferrytyna",            "ferritin"),
            Map.entry("crp",                  "crp"),
            Map.entry("tsh",                  "tsh"),
            Map.entry("wbc",                  "wbc"),
            Map.entry("rbc",                  "rbc"),
            Map.entry("hgb",                  "hemoglobin"),
            Map.entry("hct",                  "hematocrit"),
            Map.entry("mcv",                  "mcv"),
            Map.entry("mch",                  "mch"),
            Map.entry("mchc",                 "mchc"),
            Map.entry("plt",                  "platelets"),
            Map.entry("neutrofile",           "neutrophils_pct"),
            Map.entry("limfocyty",            "lymphocytes_pct"),
            Map.entry("monocyty",             "monocytes_pct"),
            Map.entry("eozynofile",           "eosinophils_pct"),
            Map.entry("bazofile",             "basophils_pct"),
            Map.entry("witamina d",           "vitamin_d"),
            Map.entry("witamina b12",         "vitamin_b12"),
            Map.entry("kwas foliowy",         "folate"),
            Map.entry("homocysteina",         "homocysteine"),
            Map.entry("hba1c",               "hba1c")
    );

    private ParameterCatalog() {}

    public static Optional<ParamMeta> find(String key) {
        return Optional.ofNullable(CATALOG.get(key.toLowerCase()));
    }

    /**
     * Resolves a canonical key from either an English key or a Polish name (with or without diacritics).
     */
    public static Optional<String> resolveKey(String nameOrKey) {
        if (nameOrKey == null || nameOrKey.isBlank()) return Optional.empty();
        String lower = nameOrKey.trim().toLowerCase();
        if (CATALOG.containsKey(lower)) return Optional.of(lower);
        String normalized = normalize(lower);
        return Optional.ofNullable(POLISH_TO_KEY.get(normalized));
    }

    private static String normalize(String s) {
        return s.replace("ą", "a").replace("ć", "c").replace("ę", "e")
                .replace("ł", "l").replace("ń", "n").replace("ó", "o")
                .replace("ś", "s").replace("ź", "z").replace("ż", "z");
    }

    public static Map<String, ParamMeta> all() {
        return CATALOG;
    }
}
