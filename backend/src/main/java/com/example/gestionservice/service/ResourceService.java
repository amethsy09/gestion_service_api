package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ResourceRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ResourceResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ResourceService {
    ResourceResponse create(ResourceRequest request);
    ResourceResponse update(UUID id, ResourceRequest request);
    void delete(UUID id);
    ResourceResponse getById(UUID id);
    PageResponse<ResourceResponse> getAll(Pageable pageable);
    List<ResourceResponse> getAvailable();
}
