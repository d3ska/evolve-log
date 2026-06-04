package com.deska.evolvelog.i18n;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuscleI18nTest {

    // T18 — MuscleI18n returns correct labels for all supported locales

    @Test
    void shouldReturnPolishLabelWhenLocaleIsPl() {
        // given / when / then
        assertThat(MuscleI18n.label("chest", "pl")).isEqualTo("Klatka piersiowa");
        assertThat(MuscleI18n.label("back", "pl")).isEqualTo("Plecy");
        assertThat(MuscleI18n.label("shoulders", "pl")).isEqualTo("Barki");
        assertThat(MuscleI18n.label("biceps", "pl")).isEqualTo("Biceps");
        assertThat(MuscleI18n.label("triceps", "pl")).isEqualTo("Triceps");
        assertThat(MuscleI18n.label("quads", "pl")).isEqualTo("Czworogłowe");
        assertThat(MuscleI18n.label("hamstrings", "pl")).isEqualTo("Dwugłowe");
        assertThat(MuscleI18n.label("glutes", "pl")).isEqualTo("Pośladki");
        assertThat(MuscleI18n.label("calves", "pl")).isEqualTo("Łydki");
        assertThat(MuscleI18n.label("core", "pl")).isEqualTo("Brzuch / Core");
        assertThat(MuscleI18n.label("traps", "pl")).isEqualTo("Czworoboczny");
        assertThat(MuscleI18n.label("forearms", "pl")).isEqualTo("Przedramiona");
        assertThat(MuscleI18n.label("cardio", "pl")).isEqualTo("Cardio");
        assertThat(MuscleI18n.label("full_body", "pl")).isEqualTo("Całe ciało");
    }

    @Test
    void shouldReturnEnglishLabelWhenLocaleIsEn() {
        // given / when / then
        assertThat(MuscleI18n.label("chest", "en")).isEqualTo("Chest");
        assertThat(MuscleI18n.label("back", "en")).isEqualTo("Back");
        assertThat(MuscleI18n.label("shoulders", "en")).isEqualTo("Shoulders");
        assertThat(MuscleI18n.label("biceps", "en")).isEqualTo("Biceps");
        assertThat(MuscleI18n.label("quads", "en")).isEqualTo("Quads");
        assertThat(MuscleI18n.label("full_body", "en")).isEqualTo("Full Body");
    }

    @Test
    void shouldFallBackToEnglishWhenLocaleIsUnknown() {
        // given / when / then
        assertThat(MuscleI18n.label("chest", "de")).isEqualTo("Chest");
        assertThat(MuscleI18n.label("back", "fr")).isEqualTo("Back");
    }

    @Test
    void shouldReturnEmptyStringWhenMuscleKeyIsNull() {
        // given / when / then
        assertThat(MuscleI18n.label(null, "en")).isEmpty();
        assertThat(MuscleI18n.label(null, "pl")).isEmpty();
    }

    @Test
    void shouldReturnRawKeyWhenMuscleKeyIsUnknown() {
        // given / when / then
        assertThat(MuscleI18n.label("unknown_muscle", "en")).isEqualTo("unknown_muscle");
        assertThat(MuscleI18n.label("unknown_muscle", "pl")).isEqualTo("unknown_muscle");
    }

    @Test
    void shouldBeCaseInsensitiveForMuscleKey() {
        // given / when / then
        assertThat(MuscleI18n.label("CHEST", "en")).isEqualTo("Chest");
        assertThat(MuscleI18n.label("Shoulders", "pl")).isEqualTo("Barki");
    }
}
