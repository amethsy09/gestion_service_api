package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.SpecialtyRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.SpecialtyResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SpecialtyService {
    SpecialtyResponse create(SpecialtyRequest request);
    SpecialtyResponse update(UUID id, SpecialtyRequest request);
    void delete(UUID id);
    SpecialtyResponse getById(UUID id);
    PageResponse<SpecialtyResponse> getAll(Pageable pageable);
}
