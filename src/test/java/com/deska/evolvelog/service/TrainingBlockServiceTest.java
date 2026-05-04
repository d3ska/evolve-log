package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.TrainingBlock;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateTrainingBlockRequest;
import com.deska.evolvelog.dto.request.UpdateTrainingBlockRequest;
import com.deska.evolvelog.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingBlockServiceTest {

    @Mock
    TrainingBlockRepository blockRepository;

    @Mock
    TrainingPlanRepository planRepository;

    @InjectMocks
    TrainingBlockService service;

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

    private TrainingBlock buildBlock(UUID userId, String name) {
        return TrainingBlock.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(name)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_storesBlockWithCorrectFields() {
        var req = new CreateTrainingBlockRequest("Raport 55", "Hypertrophy phase");
        var saved = buildBlock(user.getId(), "Raport 55");
        when(blockRepository.save(any())).thenReturn(saved);

        var dto = service.create(user, req);

        assertThat(dto.name()).isEqualTo("Raport 55");
        assertThat(dto.isActive()).isTrue();
        verify(blockRepository).save(any());
    }

    @Test
    void list_returnsOnlyCallersBlocks() {
        var block = buildBlock(user.getId(), "My Block");
        when(blockRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(block));

        var result = service.list(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("My Block");
    }

    @Test
    void update_renamesBlock() {
        var block = buildBlock(user.getId(), "Old Name");
        when(blockRepository.findByIdAndUserId(block.getId(), user.getId())).thenReturn(Optional.of(block));
        when(blockRepository.save(any())).thenReturn(block);

        var req = new UpdateTrainingBlockRequest("New Name", null, null);
        var dto = service.update(user, block.getId(), req);

        assertThat(dto).isNotNull();
        verify(blockRepository).save(block);
    }

    @Test
    void update_togglesIsActive() {
        var block = buildBlock(user.getId(), "Block");
        when(blockRepository.findByIdAndUserId(block.getId(), user.getId())).thenReturn(Optional.of(block));
        when(blockRepository.save(any())).thenReturn(block);

        service.update(user, block.getId(), new UpdateTrainingBlockRequest(null, null, false));

        verify(blockRepository).save(block);
    }

    @Test
    void update_unknownId_throws404() {
        UUID unknownId = UUID.randomUUID();
        when(blockRepository.findByIdAndUserId(unknownId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(user, unknownId, new UpdateTrainingBlockRequest("X", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_otherUsersBlock_throws404() {
        UUID otherId = UUID.randomUUID();
        when(blockRepository.findByIdAndUserId(otherId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(user, otherId, new UpdateTrainingBlockRequest("X", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_keepPlans_deletesBlockOnly() {
        var block = buildBlock(user.getId(), "Block");
        when(blockRepository.findByIdAndUserId(block.getId(), user.getId())).thenReturn(Optional.of(block));

        service.delete(user, block.getId(), false);

        verify(planRepository, never()).deleteByBlockIdAndUserId(any(), any());
        verify(blockRepository).delete(block);
    }

    @Test
    void delete_withDeletePlans_deletesPlansAndBlock() {
        var block = buildBlock(user.getId(), "Block");
        when(blockRepository.findByIdAndUserId(block.getId(), user.getId())).thenReturn(Optional.of(block));

        service.delete(user, block.getId(), true);

        verify(planRepository).deleteByBlockIdAndUserId(block.getId(), user.getId());
        verify(blockRepository).delete(block);
    }

    @Test
    void delete_otherUsersBlock_throws404() {
        UUID otherId = UUID.randomUUID();
        when(blockRepository.findByIdAndUserId(otherId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(user, otherId, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
