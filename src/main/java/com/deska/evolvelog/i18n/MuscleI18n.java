package com.deska.evolvelog.i18n;

import java.util.Map;

/**
 * Static lookup table for muscle group display names.
 * Keys are the raw {@code primary_muscle} values stored in the database (lowercase).
 */
public final class MuscleI18n {

    private static final Map<String, Map<String, String>> NAMES = Map.ofEntries(
            Map.entry("chest",      Map.of("en", "Chest",      "pl", "Klatka piersiowa")),
            Map.entry("back",       Map.of("en", "Back",       "pl", "Plecy")),
            Map.entry("shoulders",  Map.of("en", "Shoulders",  "pl", "Barki")),
            Map.entry("biceps",     Map.of("en", "Biceps",     "pl", "Biceps")),
            Map.entry("triceps",    Map.of("en", "Triceps",    "pl", "Triceps")),
            Map.entry("quads",      Map.of("en", "Quads",      "pl", "Czworogłowe")),
            Map.entry("hamstrings", Map.of("en", "Hamstrings", "pl", "Dwugłowe")),
            Map.entry("glutes",     Map.of("en", "Glutes",     "pl", "Pośladki")),
            Map.entry("calves",     Map.of("en", "Calves",     "pl", "Łydki")),
            Map.entry("core",       Map.of("en", "Core",       "pl", "Brzuch / Core")),
            Map.entry("traps",      Map.of("en", "Traps",      "pl", "Czworoboczny")),
            Map.entry("forearms",   Map.of("en", "Forearms",   "pl", "Przedramiona")),
            Map.entry("cardio",     Map.of("en", "Cardio",     "pl", "Cardio")),
            Map.entry("full_body",  Map.of("en", "Full Body",  "pl", "Całe ciało"))
    );

    private MuscleI18n() {}

    /**
     * Returns the translated display name for {@code muscleKey} in the given {@code locale}.
     * Falls back to English if the locale has no mapping, then to the raw key if unknown.
     */
    public static String label(String muscleKey, String locale) {
        if (muscleKey == null) return "";
        Map<String, String> translations = NAMES.get(muscleKey.toLowerCase());
        if (translations == null) return muscleKey;
        return translations.getOrDefault(locale, translations.getOrDefault("en", muscleKey));
    }
}
