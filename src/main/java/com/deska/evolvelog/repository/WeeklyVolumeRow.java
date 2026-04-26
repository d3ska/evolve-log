package com.deska.evolvelog.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projection for weekly volume native SQL query.
 */
public interface WeeklyVolumeRow {
    LocalDate getWeekStart();
    String getMuscle();
    BigDecimal getVolumeLoad();
    Long getSessionCount();
}
