package com.deska.evolvelog.service;

import com.deska.evolvelog.dto.response.AutoLinkResultDto;
import com.deska.evolvelog.repository.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ExerciseAutoLinkService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseAutoLinkService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public AutoLinkResultDto autoLink(UUID userId) {
        int unlinkedBefore = exerciseRepository.findUnlinkedByUserId(userId).size();
        int matched = exerciseRepository.bulkAutoLinkByUserId(userId);
        int skipped = unlinkedBefore - matched;
        return new AutoLinkResultDto(matched, skipped);
    }
}
