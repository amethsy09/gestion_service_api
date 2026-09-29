package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.ResourceRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ResourceResponse;
import com.example.gestionservice.service.ResourceService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Gestion des ressources humaines")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Créer une ressource")
    public ResponseEntity<ApiResponse<ResourceResponse>> create(
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ressource créée avec succès", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Lister toutes les ressources")
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAll(PageRequest.of(page, size, Sort.by("lastName").ascending()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Récupérer une ressource par ID")
    public ResponseEntity<ApiResponse<ResourceResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Modifier une ressource")
    public ResponseEntity<ApiResponse<ResourceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ressource modifiée avec succès", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une ressource")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Ressource supprimée avec succès", null));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Lister les ressources disponibles")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success(service.getAvailable()));
    }
}
