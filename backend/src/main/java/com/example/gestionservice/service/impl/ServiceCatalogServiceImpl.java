package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.ServiceCatalogRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ServiceCatalogResponse;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.ServiceCatalogMapper;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.example.gestionservice.service.ServiceCatalogService;
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
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServiceCatalogRepository repository;
    private final ServiceCatalogMapper mapper;

    @Override
    @Transactional
    public ServiceCatalogResponse create(ServiceCatalogRequest request) {
        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Un service avec le nom '" + request.getName() + "' existe déjà");
        }
        ServiceCatalog entity = mapper.toEntity(request);
        entity = repository.save(entity);
        log.info("Service créé : id={}, name={}", entity.getId(), entity.getName());
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ServiceCatalogResponse update(UUID id, ServiceCatalogRequest request) {
        ServiceCatalog entity = findOrThrow(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new BusinessException("Un service avec le nom '" + request.getName() + "' existe déjà");
        }
        mapper.updateEntity(request, entity);
        entity = repository.save(entity);
        log.info("Service modifié : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ServiceCatalog entity = findOrThrow(id);
        repository.delete(entity);
        log.info("Service supprimé : id={}", id);
    }

    @Override
    public ServiceCatalogResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<ServiceCatalogResponse> getAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    public List<ServiceCatalogResponse> getActive() {
        return repository.findByActiveTrue().stream()
                .map(mapper::toResponse)
                .toList();
    }

    private ServiceCatalog findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCatalog", id));
    }
}
