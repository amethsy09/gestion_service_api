package com.example.gestionservice.controller;

import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ServiceRequestResponse;
import com.example.gestionservice.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/service-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Service Requests", description = "Administration des demandes de service")
@SecurityRequirement(name = "bearerAuth")
public class AdminServiceRequestController {

    private final ServiceRequestService service;

    @GetMapping
    @Operation(summary = "Toutes les demandes — ADMIN uniquement")
    public ResponseEntity<ApiResponse<PageResponse<ServiceRequestResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ServiceRequestResponse> result = service.getAllRequests(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
