package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.ResponsibleRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ResponsibleResponse;
import com.example.gestionservice.entity.Responsible;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.ResponsibleMapper;
import com.example.gestionservice.repository.ResponsibleRepository;
import com.example.gestionservice.service.ResponsibleService;
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
public class ResponsibleServiceImpl implements ResponsibleService {

    private final ResponsibleRepository repository;
    private final ResponsibleMapper mapper;

    @Override
    @Transactional
    public ResponsibleResponse create(ResponsibleRequest request) {
        if (repository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Un responsable avec l'email '" + request.getEmail() + "' existe déjà");
        }
        Responsible entity = mapper.toEntity(request);
        entity = repository.save(entity);
        log.info("Responsable créé : id={}, email={}", entity.getId(), entity.getEmail());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ResponsibleResponse update(UUID id, ResponsibleRequest request) {
        Responsible entity = findOrThrow(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("Un responsable avec l'email '" + request.getEmail() + "' existe déjà");
        }
        mapper.updateEntity(request, entity);
        entity = repository.save(entity);
        log.info("Responsable modifié : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Responsible entity = findOrThrow(id);
        repository.delete(entity);
        log.info("Responsable supprimé : id={}", id);
    }

    @Override
    public ResponsibleResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<ResponsibleResponse> getAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable).map(mapper::toResponse));
    }

    private Responsible findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responsible", id));
    }
}
