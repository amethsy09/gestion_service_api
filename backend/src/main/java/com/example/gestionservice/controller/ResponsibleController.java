package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.ResponsibleRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.ResponsibleResponse;
import com.example.gestionservice.service.ResponsibleAccountService;
import com.example.gestionservice.service.ResponsibleService;
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
@RequestMapping("/api/v1/responsibles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Responsibles", description = "Gestion des responsables de prestation")
@SecurityRequirement(name = "bearerAuth")
public class ResponsibleController {

    private final ResponsibleService service;
    private final ResponsibleAccountService accountService;

    @PostMapping
    @Operation(summary = "Créer un responsable")
    public ResponseEntity<ApiResponse<ResponsibleResponse>> create(
            @Valid @RequestBody ResponsibleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Responsable créé avec succès", accountService.createWithTemporaryPassword(request)));
    }

    @GetMapping
    @Operation(summary = "Lister tous les responsables")
    public ResponseEntity<ApiResponse<PageResponse<ResponsibleResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAll(PageRequest.of(page, size, Sort.by("lastName").ascending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un responsable par ID")
    public ResponseEntity<ApiResponse<ResponsibleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un responsable")
    public ResponseEntity<ApiResponse<ResponsibleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ResponsibleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Responsable modifié avec succès", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un responsable")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Responsable supprimé avec succès", null));
    }
}
