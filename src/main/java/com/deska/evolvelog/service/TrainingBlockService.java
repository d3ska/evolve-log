package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.TrainingBlock;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateTrainingBlockRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingBlockRequest;
import com.deska.evolvelog.dto.response.TrainingBlockDto;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.TrainingBlockRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingBlockService {

    private final TrainingBlockRepository blockRepository;
    private final TrainingPlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<TrainingBlockDto> list(User user) {
        return blockRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(TrainingBlockDto::from)
                .toList();
    }

    @Transactional
    public TrainingBlockDto create(User user, CreateTrainingBlockRequest request) {
        TrainingBlock block = TrainingBlock.builder()
                .userId(user.getId())
                .name(request.name())
                .description(request.description())
                .build();
        return TrainingBlockDto.from(blockRepository.save(block));
    }

    @Transactional
    public TrainingBlockDto update(User user, UUID id, UpdateTrainingBlockRequest request) {
        TrainingBlock block = blockRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TrainingBlock", id));
        block.applyPatch(request.name(), request.description(), request.isActive());
        return TrainingBlockDto.from(blockRepository.save(block));
    }

    @Transactional
    public void delete(User user, UUID id, boolean deletePlans) {
        TrainingBlock block = blockRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TrainingBlock", id));
        if (deletePlans) {
            planRepository.deleteByBlockIdAndUserId(id, user.getId());
        }
        blockRepository.delete(block);
    }
}
