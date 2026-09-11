package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.PrestationRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.PrestationResourceResponse;
import com.example.gestionservice.dto.response.PrestationResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PrestationService {
    PrestationResponse create(PrestationRequest request);
    PrestationResponse getById(UUID id);
    PageResponse<PrestationResponse> getAll(Pageable pageable);
    PrestationResponse start(UUID id);
    PrestationResponse complete(UUID id);
    PrestationResponse cancel(UUID id);
    PrestationResponse plan(UUID id);

    // Affectation ressources
    PrestationResourceResponse assignResource(UUID prestationId, UUID resourceId);
    void removeResource(UUID prestationId, UUID resourceId);
    List<PrestationResourceResponse> getResources(UUID prestationId);
}
