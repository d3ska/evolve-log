package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;

public interface HealthMetricProvider {

    /**
     * Unique identifier for the data source, e.g. "withings", "garmin", "apple_health".
     */
    String source();

    /**
     * Pulls the latest measurements from the provider and persists them.
     *
     * @return number of individual metric data points upserted
     */
    int sync(User user);
}
