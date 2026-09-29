package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ServiceCatalogRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ServiceCatalogResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ServiceCatalogService {
    ServiceCatalogResponse create(ServiceCatalogRequest request);
    ServiceCatalogResponse update(UUID id, ServiceCatalogRequest request);
    void delete(UUID id);
    ServiceCatalogResponse getById(UUID id);
    PageResponse<ServiceCatalogResponse> getAll(Pageable pageable);
    List<ServiceCatalogResponse> getActive();
}
