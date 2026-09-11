package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.PrestationRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.PrestationResourceResponse;
import com.example.gestionservice.dto.response.PrestationResponse;
import com.example.gestionservice.service.PrestationService;
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
@RequestMapping("/api/v1/prestations")
@RequiredArgsConstructor
@Tag(name = "Prestations", description = "Gestion des prestations")
@SecurityRequirement(name = "bearerAuth")
public class PrestationController {

    private final PrestationService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Créer une prestation")
    public ResponseEntity<ApiResponse<PrestationResponse>> create(
            @Valid @RequestBody PrestationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prestation créée avec succès", service.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Lister toutes les prestations")
    public ResponseEntity<ApiResponse<PageResponse<PrestationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Récupérer une prestation par ID")
    public ResponseEntity<ApiResponse<PrestationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PutMapping("/{id}/plan")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Planifier une prestation (PAID → PLANNED)")
    public ResponseEntity<ApiResponse<PrestationResponse>> plan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Prestation planifiée", service.plan(id)));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Démarrer une prestation (PLANNED → IN_PROGRESS)")
    public ResponseEntity<ApiResponse<PrestationResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Prestation démarrée", service.start(id)));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Terminer une prestation (IN_PROGRESS → COMPLETED)")
    public ResponseEntity<ApiResponse<PrestationResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Prestation terminée", service.complete(id)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Annuler une prestation")
    public ResponseEntity<ApiResponse<PrestationResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Prestation annulée", service.cancel(id)));
    }

    // ===== AFFECTATION RESSOURCES =====

    @PostMapping("/{prestationId}/resources/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Affecter une ressource à une prestation")
    public ResponseEntity<ApiResponse<PrestationResourceResponse>> assignResource(
            @PathVariable UUID prestationId,
            @PathVariable UUID resourceId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ressource affectée avec succès",
                        service.assignResource(prestationId, resourceId)));
    }

    @DeleteMapping("/{prestationId}/resources/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Retirer une ressource d'une prestation")
    public ResponseEntity<ApiResponse<Void>> removeResource(
            @PathVariable UUID prestationId,
            @PathVariable UUID resourceId) {
        service.removeResource(prestationId, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Ressource retirée avec succès", null));
    }

    @GetMapping("/{prestationId}/resources")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Lister les ressources d'une prestation")
    public ResponseEntity<ApiResponse<List<PrestationResourceResponse>>> getResources(
            @PathVariable UUID prestationId) {
        return ResponseEntity.ok(ApiResponse.success(service.getResources(prestationId)));
    }
}
