package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO de création d'une demande de service.
 * IMPORTANT : accountId est absent — il est extrait automatiquement du JWT.
 */
@Getter
@Setter
public class ServiceRequestCreateRequest {

    @NotNull(message = "L'identifiant du service est obligatoire")
    private UUID serviceId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 255, message = "Le titre doit contenir entre 3 et 255 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;
}
