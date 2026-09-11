package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.ServiceRequestCreateRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ServiceRequestResponse;
import com.example.gestionservice.security.JwtAuthenticationPrincipal;
import com.example.gestionservice.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
@Tag(name = "Service Requests", description = "Gestion des demandes de service")
@SecurityRequirement(name = "bearerAuth")
public class ServiceRequestController {

    private final ServiceRequestService service;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer une demande de service — accountId extrait du JWT")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> create(
            @Valid @RequestBody ServiceRequestCreateRequest request,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        ServiceRequestResponse response = service.create(request, principal.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande créée avec succès", response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mes demandes (filtrées par accountId JWT)")
    public ResponseEntity<ApiResponse<PageResponse<ServiceRequestResponse>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        PageResponse<ServiceRequestResponse> result = service.getMyRequests(
                principal.getAccountId(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Détail d'une demande — l'utilisateur ne voit que la sienne")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id, principal.getAccountId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Annuler une demande")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
        service.cancel(id, principal.getAccountId());
        return ResponseEntity.ok(ApiResponse.success("Demande annulée", null));
    }
}
