package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.HealthMetric;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.repository.HealthMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthMetricService {

    private final HealthMetricRepository repository;

    @Transactional
    public void upsert(User user, String source, LocalDate date,
                       String metricKey, BigDecimal value, String unit) {
        repository.upsert(user.getId(), source, date, metricKey, value, unit, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<DailyHealthMetricsDto> getDailyMetrics(UUID userId, String source,
                                                        LocalDate from, LocalDate to) {
        List<HealthMetric> rows = (from != null && to != null)
                ? repository.findByUserIdAndSourceAndDateBetweenOrderByDateAsc(userId, source, from, to)
                : repository.findByUserIdAndSourceOrderByDateDesc(userId, source);
        return groupByDate(rows);
    }

    @Transactional(readOnly = true)
    public List<DailyHealthMetricsDto> getAllDailyMetrics(UUID userId, LocalDate from, LocalDate to) {
        List<HealthMetric> rows = repository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
        return groupByDate(rows);
    }

    private List<DailyHealthMetricsDto> groupByDate(List<HealthMetric> rows) {
        record Key(LocalDate date, String source) {}
        Map<Key, Map<String, BigDecimal>> grouped = new LinkedHashMap<>();
        for (HealthMetric m : rows) {
            grouped
                .computeIfAbsent(new Key(m.getDate(), m.getSource()), k -> new LinkedHashMap<>())
                .put(m.getMetricKey(), m.getValue());
        }
        return grouped.entrySet().stream()
                .map(e -> new DailyHealthMetricsDto(e.getKey().date(), e.getKey().source(), e.getValue()))
                .toList();
    }
}
