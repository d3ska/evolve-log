package com.deska.evolvelog.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure static utility for training volume and e1RM calculations.
 * No Spring dependencies — fully unit-testable.
 */
public final class VolumeCalculator {

    private VolumeCalculator() {}

    /**
     * Volume Load = sets × reps × weightKg.
     * Returns null when any input is null (bodyweight or incomplete exercises).
     */
    public static BigDecimal volumeLoad(Integer sets, Integer reps, BigDecimal weightKg) {
        if (sets == null || reps == null || weightKg == null) return null;
        return weightKg.multiply(BigDecimal.valueOf((long) sets * reps));
    }

    /**
     * Internal Load = volumeLoad × rpe.
     * Returns null when either input is null.
     */
    public static BigDecimal internalLoad(BigDecimal volumeLoad, BigDecimal rpe) {
        if (volumeLoad == null || rpe == null) return null;
        return volumeLoad.multiply(rpe).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Epley estimated 1-Rep Maximum: weightKg × (1 + reps / 30.0).
     * Returns null when reps > 12 (formula diverges for high reps).
     * Returns weightKg as-is when reps == 1.
     */
    public static BigDecimal epleyE1RM(BigDecimal weightKg, int reps) {
        if (weightKg == null) return null;
        if (reps > 12) return null;
        if (reps == 1) return weightKg;
        BigDecimal multiplier = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30.0), 6, RoundingMode.HALF_UP));
        return weightKg.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
