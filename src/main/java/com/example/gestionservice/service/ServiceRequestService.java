package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ServiceRequestCreateRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ServiceRequestResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceRequestService {
    ServiceRequestResponse create(ServiceRequestCreateRequest request, UUID accountId);
    ServiceRequestResponse getById(UUID id, UUID accountId);
    PageResponse<ServiceRequestResponse> getMyRequests(UUID accountId, Pageable pageable);
    PageResponse<ServiceRequestResponse> getAllRequests(Pageable pageable);  // ADMIN
    void cancel(UUID id, UUID accountId);
}
