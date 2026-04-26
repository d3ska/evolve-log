package com.deska.evolvelog.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeCalculatorTest {

    // ── volumeLoad ──────────────────────────────────────────────────────────

    @Test
    void shouldReturnNullWhenWeightIsNull() {
        // given / when / then
        assertThat(VolumeCalculator.volumeLoad(3, 8, null)).isNull();
    }

    @Test
    void shouldCalculateVolumeLoad() {
        // given
        BigDecimal weight = new BigDecimal("100.00");
        // when
        BigDecimal result = VolumeCalculator.volumeLoad(3, 8, weight);
        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("2400.00"));
    }

    @Test
    void shouldCalculateVolumeLoadWithDecimalWeight() {
        // given
        BigDecimal weight = new BigDecimal("22.50");
        // when
        BigDecimal result = VolumeCalculator.volumeLoad(4, 10, weight);
        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    // ── internalLoad ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnNullWhenVolumeLoadIsNull() {
        // given / when / then
        assertThat(VolumeCalculator.internalLoad(null, new BigDecimal("7.5"))).isNull();
    }

    @Test
    void shouldReturnNullWhenRpeIsNull() {
        // given / when / then
        assertThat(VolumeCalculator.internalLoad(new BigDecimal("2400"), null)).isNull();
    }

    @Test
    void shouldCalculateInternalLoad() {
        // given
        BigDecimal volumeLoad = new BigDecimal("2400");
        BigDecimal rpe = new BigDecimal("8.0");
        // when
        BigDecimal result = VolumeCalculator.internalLoad(volumeLoad, rpe);
        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("19200.00"));
    }

    // ── epleyE1RM ────────────────────────────────────────────────────────────

    @Test
    void shouldReturnWeightKgWhenRepsIsOne() {
        // given
        BigDecimal weight = new BigDecimal("140.00");
        // when
        BigDecimal result = VolumeCalculator.epleyE1RM(weight, 1);
        // then
        assertThat(result).isEqualByComparingTo(weight);
    }

    @Test
    void shouldCalculateE1RMForRepsEqualTwelve() {
        // given
        BigDecimal weight = new BigDecimal("100.00");
        // when: weight * (1 + 12/30) = 100 * 1.4 = 140
        BigDecimal result = VolumeCalculator.epleyE1RM(weight, 12);
        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("140.00"));
    }

    @Test
    void shouldReturnNullWhenRepsExceedTwelve() {
        // given / when / then
        assertThat(VolumeCalculator.epleyE1RM(new BigDecimal("100"), 13)).isNull();
    }

    @Test
    void shouldReturnNullWhenWeightIsNullForE1RM() {
        // given / when / then
        assertThat(VolumeCalculator.epleyE1RM(null, 5)).isNull();
    }

    @Test
    void shouldCalculateE1RMForTypicalSet() {
        // given: 5 reps at 100kg → 100 * (1 + 5/30) = 100 * 1.1667 ≈ 116.67
        BigDecimal weight = new BigDecimal("100.00");
        // when
        BigDecimal result = VolumeCalculator.epleyE1RM(weight, 5);
        // then
        assertThat(result).isNotNull();
        assertThat(result).isGreaterThan(new BigDecimal("116.00"));
        assertThat(result).isLessThan(new BigDecimal("117.00"));
    }
}
