package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.ServiceCatalogRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ServiceCatalogResponse;
import com.example.gestionservice.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Gestion du catalogue de services")
public class ServiceCatalogController {

    private final ServiceCatalogService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un service")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> create(
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service créé avec succès", service.create(request)));
    }

    @GetMapping
    @Operation(summary = "Lister tous les services avec pagination")
    public ResponseEntity<ApiResponse<PageResponse<ServiceCatalogResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageResponse<ServiceCatalogResponse> result = service.getAll(PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un service par ID")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un service")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Service modifié avec succès", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un service")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Service supprimé avec succès", null));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les services actifs")
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(service.getActive()));
    }
}
