package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateMeasurementRequest;
import com.deska.evolvelog.dto.request.UpdateMeasurementRequest;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.MeasurementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;

    public MeasurementService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    @Transactional
    public Measurement create(User user, CreateMeasurementRequest request) {
        Measurement measurement = Measurement.builder()
                .user(user)
                .date(request.date())
                .weightKg(request.weightKg())
                .bodyFatPercent(request.bodyFatPercent())
                .chestCm(request.chestCm())
                .waistNarrowestCm(request.waistNarrowestCm())
                .waistNavelCm(request.waistNavelCm())
                .bicepsCm(request.bicepsCm())
                .thighCm(request.thighCm())
                .calvesCm(request.calvesCm())
                .notes(request.notes())
                .build();
        return measurementRepository.save(measurement);
    }

    @Transactional(readOnly = true)
    public Page<Measurement> findAll(UUID userId, int page, int size) {
        return measurementRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Measurement findById(UUID id, UUID userId) {
        return measurementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Measurement", id));
    }

    @Transactional
    public Measurement update(UUID id, UUID userId, UpdateMeasurementRequest request) {
        Measurement measurement = findById(id, userId);
        measurement.applyPatch(request.date(), request.weightKg(), request.bodyFatPercent(),
                request.chestCm(), request.waistNarrowestCm(), request.waistNavelCm(),
                request.bicepsCm(), request.thighCm(), request.calvesCm(), request.notes());
        return measurementRepository.save(measurement);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        Measurement measurement = findById(id, userId);
        measurementRepository.delete(measurement);
    }
}
