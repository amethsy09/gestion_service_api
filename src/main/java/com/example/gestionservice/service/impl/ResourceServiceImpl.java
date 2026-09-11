package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.ResourceRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ResourceResponse;
import com.example.gestionservice.entity.Resource;
import com.example.gestionservice.entity.Specialty;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.ResourceMapper;
import com.example.gestionservice.repository.ResourceRepository;
import com.example.gestionservice.repository.SpecialtyRepository;
import com.example.gestionservice.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository repository;
    private final SpecialtyRepository specialtyRepository;
    private final ResourceMapper mapper;

    @Override
    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        if (repository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Une ressource avec l'email '" + request.getEmail() + "' existe déjà");
        }
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialty", request.getSpecialtyId()));

        Resource entity = mapper.toEntity(request);
        entity.setSpecialty(specialty);
        entity = repository.save(entity);
        log.info("Ressource créée : id={}, email={}, specialty={}", entity.getId(), entity.getEmail(), specialty.getName());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource entity = findOrThrow(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("Une ressource avec l'email '" + request.getEmail() + "' existe déjà");
        }
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialty", request.getSpecialtyId()));

        mapper.updateEntity(request, entity);
        entity.setSpecialty(specialty);
        entity = repository.save(entity);
        log.info("Ressource modifiée : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Resource entity = findOrThrow(id);
        repository.delete(entity);
        log.info("Ressource supprimée : id={}", id);
    }

    @Override
    public ResourceResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<ResourceResponse> getAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    public List<ResourceResponse> getAvailable() {
        return repository.findAllAvailable().stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Resource findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }
}
