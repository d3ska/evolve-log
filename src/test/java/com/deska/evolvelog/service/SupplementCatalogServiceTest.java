package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Supplement;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateSupplementRequest;
import com.deska.evolvelog.dto.response.SupplementDto;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementCatalogServiceTest {

    @Mock
    SupplementRepository supplementRepository;

    @InjectMocks
    SupplementCatalogService service;

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
    void shouldReturnDtoWhenCreatingSupplement() {
        // given
        var req = new CreateSupplementRequest("Vitamin D", "Nature's Best", "capsule", "500 IU");
        var saved = Supplement.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Vitamin D")
                .brand("Nature's Best")
                .form("capsule")
                .notes("500 IU")
                .build();
        when(supplementRepository.save(any())).thenReturn(saved);

        // when
        SupplementDto result = service.createSupplement(user, req);

        // then
        assertThat(result.name()).isEqualTo("Vitamin D");
        assertThat(result.brand()).isEqualTo("Nature's Best");
        verify(supplementRepository).save(any(Supplement.class));
    }

    @Test
    void shouldCallDeleteWhenDeletingExistingSupplement() {
        // given
        UUID id = UUID.randomUUID();
        var supplement = Supplement.builder().id(id).user(user).name("Creatine").build();
        when(supplementRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(supplement));

        // when
        service.deleteSupplement(id, user.getId());

        // then
        verify(supplementRepository).delete(supplement);
    }

    @Test
    void shouldThrow404WhenDeletingSupplementWithUnknownId() {
        // given
        UUID id = UUID.randomUUID();
        when(supplementRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.deleteSupplement(id, user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
