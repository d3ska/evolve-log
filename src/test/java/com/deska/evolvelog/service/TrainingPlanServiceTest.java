package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.TrainingBlock;
import com.deska.evolvelog.domain.TrainingPlan;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateTrainingPlanRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingPlanRequest;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.ExerciseDefinitionRepository;
import com.deska.evolvelog.repository.PlannedExerciseRepository;
import com.deska.evolvelog.repository.TrainingBlockRepository;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    @Mock TrainingPlanRepository planRepository;
    @Mock PlannedExerciseRepository exerciseRepository;
    @Mock ExerciseDefinitionRepository definitionRepository;
    @Mock TrainingBlockRepository blockRepository;

    @InjectMocks
    TrainingPlanService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private TrainingBlock buildBlock(UUID userId) {
        return TrainingBlock.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Block")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private TrainingPlan buildPlan(User planUser, TrainingBlock block) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID())
                .user(planUser)
                .name("Plan")
                .isActive(true)
                .block(block)
                .build();
    }

    @Test
    void create_withValidBlockId_setsBlockOnPlan() {
        var block = buildBlock(user.getId());
        var req = new CreateTrainingPlanRequest("Plan", null, null, block.getId(), null);

        when(blockRepository.findByIdAndUserId(block.getId(), user.getId())).thenReturn(Optional.of(block));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.create(user, req);

        assert saved.getBlockId().equals(block.getId());
        verify(blockRepository).findByIdAndUserId(block.getId(), user.getId());
    }

    @Test
    void create_withBlockIdOfAnotherUser_throws404() {
        UUID foreignBlockId = UUID.randomUUID();
        var req = new CreateTrainingPlanRequest("Plan", null, null, foreignBlockId, null);

        when(blockRepository.findByIdAndUserId(foreignBlockId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(user, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withExplicitNullBlockId_clearsFk() {
        var block = buildBlock(user.getId());
        var plan = buildPlan(user, block);

        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new UpdateTrainingPlanRequest(null, null, null, null, Optional.empty(), null);
        var updated = service.update(plan.getId(), user.getId(), req);

        assert updated.getBlockId() == null;
        verify(planRepository).save(plan);
    }

    @Test
    void update_withBlockIdOfAnotherUser_throws404() {
        var plan = buildPlan(user, null);
        UUID foreignBlockId = UUID.randomUUID();

        when(planRepository.findByIdAndUserId(plan.getId(), user.getId())).thenReturn(Optional.of(plan));
        when(blockRepository.findByIdAndUserId(foreignBlockId, user.getId())).thenReturn(Optional.empty());

        var req = new UpdateTrainingPlanRequest(null, null, null, null, Optional.of(foreignBlockId), null);

        assertThatThrownBy(() -> service.update(plan.getId(), user.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
