package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.TrainingPlanVersion;
import com.deska.evolvelog.exception.ResourceNotFoundException;
import com.deska.evolvelog.repository.TrainingPlanRepository;
import com.deska.evolvelog.repository.TrainingPlanVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingPlanVersionServiceTest {

    @Mock
    TrainingPlanVersionRepository versionRepository;

    @Mock
    TrainingPlanRepository planRepository;

    @InjectMocks
    TrainingPlanVersionService service;

    @Test
    void listVersions_throws404_whenPlanBelongsToOtherUser() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listVersions(planId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listVersions_returnsVersions_forOwner() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TrainingPlanVersion v = TrainingPlanVersion.builder()
                .trainingPlanId(planId).version(1).exercises("[]").build();

        when(planRepository.findByIdAndUserId(planId, userId))
                .thenReturn(Optional.of(com.deska.evolvelog.domain.TrainingPlan.builder()
                        .id(planId).name("Plan").build()));
        when(versionRepository.findByTrainingPlanIdOrderByVersionAsc(planId)).thenReturn(List.of(v));

        List<TrainingPlanVersion> result = service.listVersions(planId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(1);
    }

    @Test
    void getVersion_throws404_whenPlanBelongsToOtherUser() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(planRepository.findByIdAndUserId(planId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVersion(planId, 1, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVersion_throws404_whenVersionDoesNotExist() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(planRepository.findByIdAndUserId(planId, userId))
                .thenReturn(Optional.of(com.deska.evolvelog.domain.TrainingPlan.builder()
                        .id(planId).name("Plan").build()));
        when(versionRepository.findByTrainingPlanIdAndVersion(planId, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVersion(planId, 99, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
