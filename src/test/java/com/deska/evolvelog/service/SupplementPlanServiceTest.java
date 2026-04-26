package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.*;
import com.deska.evolvelog.dto.request.CreateSupplementPlanEntryRequest;
import com.deska.evolvelog.dto.request.CreateSupplementPlanRequest;
import com.deska.evolvelog.dto.response.SupplementPlanDto;
import com.deska.evolvelog.repository.SupplementPlanEntryRepository;
import com.deska.evolvelog.repository.SupplementPlanRepository;
import com.deska.evolvelog.repository.SupplementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementPlanServiceTest {

    @Mock
    SupplementPlanRepository planRepository;

    @Mock
    SupplementPlanEntryRepository entryRepository;

    @Mock
    SupplementRepository supplementRepository;

    @InjectMocks
    SupplementPlanService service;

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

    @Test
    void shouldReturnDtoWhenCreatingPlan() {
        // given
        var req = new CreateSupplementPlanRequest("Morning Stack", "Daily morning supplements");
        var saved = SupplementPlan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Morning Stack")
                .description("Daily morning supplements")
                .build();
        when(planRepository.save(any())).thenReturn(saved);

        // when
        SupplementPlanDto result = service.createPlan(user, req);

        // then
        assertThat(result.name()).isEqualTo("Morning Stack");
        assertThat(result.description()).isEqualTo("Daily morning supplements");
        verify(planRepository).save(any(SupplementPlan.class));
    }

    @Test
    void shouldAddEntryToPlanWhenSupplementAndPlanExist() {
        // given
        UUID planId = UUID.randomUUID();
        UUID suppId = UUID.randomUUID();
        var plan = SupplementPlan.builder().id(planId).user(user).name("Stack").build();
        var supp = Supplement.builder().id(suppId).user(user).name("Creatine").build();
        var req = new CreateSupplementPlanEntryRequest(suppId, TimeSlot.MORNING, null, null, "g", null, 0);
        when(planRepository.findByIdAndUserId(planId, user.getId())).thenReturn(Optional.of(plan));
        when(supplementRepository.findByIdAndUserId(suppId, user.getId())).thenReturn(Optional.of(supp));
        when(planRepository.save(any())).thenReturn(plan);

        // when
        SupplementPlanDto result = service.addEntry(planId, user.getId(), req);

        // then
        assertThat(result).isNotNull();
        verify(planRepository).save(plan);
    }

    @Test
    void shouldThrow404WhenAddingEntryToUnknownPlan() {
        // given
        UUID planId = UUID.randomUUID();
        var req = new CreateSupplementPlanEntryRequest(UUID.randomUUID(), TimeSlot.MORNING, null, null, null, null, 0);
        when(planRepository.findByIdAndUserId(planId, user.getId())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.addEntry(planId, user.getId(), req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldRemoveEntryFromPlanWhenEntryExists() {
        // given
        UUID planId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        var entry = SupplementPlanEntry.builder().id(entryId).timeSlot(TimeSlot.MORNING).build();
        var plan = SupplementPlan.builder()
                .id(planId)
                .user(user)
                .name("Stack")
                .entries(new ArrayList<>(List.of(entry)))
                .build();
        when(planRepository.findByIdAndUserId(planId, user.getId())).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenReturn(plan);

        // when
        service.removeEntry(planId, entryId, user.getId());

        // then
        assertThat(plan.getEntries()).isEmpty();
        verify(planRepository).save(plan);
    }

    @Test
    void shouldCallDeleteWhenDeletingExistingPlan() {
        // given
        UUID planId = UUID.randomUUID();
        var plan = SupplementPlan.builder().id(planId).user(user).name("Stack").build();
        when(planRepository.findByIdAndUserId(planId, user.getId())).thenReturn(Optional.of(plan));

        // when
        service.deletePlan(planId, user.getId());

        // then
        verify(planRepository).delete(plan);
    }

    @Test
    void shouldThrow404WhenDeletingPlanWithUnknownId() {
        // given
        UUID planId = UUID.randomUUID();
        when(planRepository.findByIdAndUserId(planId, user.getId())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.deletePlan(planId, user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
