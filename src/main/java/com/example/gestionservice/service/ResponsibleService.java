package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ResponsibleRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ResponsibleResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResponsibleService {
    ResponsibleResponse create(ResponsibleRequest request);
    ResponsibleResponse update(UUID id, ResponsibleRequest request);
    void delete(UUID id);
    ResponsibleResponse getById(UUID id);
    PageResponse<ResponsibleResponse> getAll(Pageable pageable);
}
