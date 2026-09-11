package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.SpecialtyRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.SpecialtyResponse;
import com.example.gestionservice.service.SpecialtyService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
@Tag(name = "Specialties", description = "Gestion des spécialités des ressources")
@SecurityRequirement(name = "bearerAuth")
public class SpecialtyController {

    private final SpecialtyService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une spécialité")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> create(
            @Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Spécialité créée avec succès", service.create(request)));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les spécialités")
    public ResponseEntity<ApiResponse<PageResponse<SpecialtyResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAll(PageRequest.of(page, size, Sort.by("name").ascending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une spécialité par ID")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une spécialité")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Spécialité modifiée avec succès", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une spécialité")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Spécialité supprimée avec succès", null));
    }
}
