package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.Supplement;
import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.CreateSupplementRequest;
import com.deska.evolvelog.dto.response.SupplementDto;
import com.deska.evolvelog.repository.SupplementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SupplementCatalogService {

    private final SupplementRepository supplementRepository;

    public SupplementCatalogService(SupplementRepository supplementRepository) {
        this.supplementRepository = supplementRepository;
    }

    @Transactional
    public SupplementDto createSupplement(User user, CreateSupplementRequest req) {
        Supplement supplement = Supplement.builder()
                .user(user)
                .name(req.name())
                .brand(req.brand())
                .form(req.form())
                .notes(req.notes())
                .build();
        return SupplementDto.from(supplementRepository.save(supplement));
    }

    @Transactional(readOnly = true)
    public List<SupplementDto> listSupplements(UUID userId) {
        return supplementRepository.findByUserIdOrderByNameAsc(userId)
                .stream().map(SupplementDto::from).toList();
    }

    @Transactional
    public void deleteSupplement(UUID id, UUID userId) {
        Supplement s = supplementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplement not found"));
        supplementRepository.delete(s);
    }
}
