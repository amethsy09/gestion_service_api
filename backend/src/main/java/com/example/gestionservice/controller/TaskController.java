package com.example.gestionservice.controller;

import com.example.gestionservice.dto.request.TaskRequest;
import com.example.gestionservice.dto.request.TaskStatusUpdateRequest;
import com.example.gestionservice.dto.response.ApiResponse;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.TaskResponse;
import com.example.gestionservice.service.TaskService;
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
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Gestion des tâches")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService service;

    @PostMapping("/api/v1/prestations/{prestationId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Créer une tâche pour une prestation")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @PathVariable UUID prestationId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tâche créée avec succès", service.create(prestationId, request)));
    }

    @GetMapping("/api/v1/tasks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Récupérer une tâche par ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping("/api/v1/prestations/{prestationId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Lister les tâches d'une prestation")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getByPrestation(
            @PathVariable UUID prestationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getByPrestation(prestationId,
                        PageRequest.of(page, size, Sort.by("dueDate").ascending()))));
    }

    @GetMapping("/api/v1/resources/{resourceId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Lister les tâches d'une ressource")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getByResource(
            @PathVariable UUID resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getByResource(resourceId,
                        PageRequest.of(page, size, Sort.by("dueDate").ascending()))));
    }

    @PatchMapping("/api/v1/tasks/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Mettre à jour le statut d'une tâche")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", service.updateStatus(id, request)));
    }

    @PostMapping("/api/v1/tasks/{taskId}/assign/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN','RESPONSIBLE')")
    @Operation(summary = "Affecter une ressource à une tâche")
    public ResponseEntity<ApiResponse<TaskResponse>> assignResource(
            @PathVariable UUID taskId,
            @PathVariable UUID resourceId) {
        return ResponseEntity.ok(ApiResponse.success("Ressource affectée à la tâche",
                service.assignResource(taskId, resourceId)));
    }
}
