package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.SpecialtyRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.SpecialtyResponse;
import com.example.gestionservice.entity.Specialty;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.SpecialtyMapper;
import com.example.gestionservice.repository.SpecialtyRepository;
import com.example.gestionservice.service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository repository;
    private final SpecialtyMapper mapper;

    @Override
    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Une spécialité avec le nom '" + request.getName() + "' existe déjà");
        }
        Specialty entity = mapper.toEntity(request);
        entity = repository.save(entity);
        log.info("Spécialité créée : id={}, name={}", entity.getId(), entity.getName());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public SpecialtyResponse update(UUID id, SpecialtyRequest request) {
        Specialty entity = findOrThrow(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new BusinessException("Une spécialité avec le nom '" + request.getName() + "' existe déjà");
        }
        mapper.updateEntity(request, entity);
        entity = repository.save(entity);
        log.info("Spécialité modifiée : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Specialty entity = findOrThrow(id);
        repository.delete(entity);
        log.info("Spécialité supprimée : id={}", id);
    }

    @Override
    public SpecialtyResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<SpecialtyResponse> getAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable).map(mapper::toResponse));
    }

    private Specialty findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty", id));
    }
}
