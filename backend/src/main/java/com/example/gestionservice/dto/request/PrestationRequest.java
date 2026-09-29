package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PrestationRequest {

    @NotNull(message = "L'identifiant de la demande de service est obligatoire")
    private UUID serviceRequestId;

    @NotNull(message = "L'identifiant du responsable est obligatoire")
    private UUID responsibleId;

    @NotBlank(message = "Le nom de la prestation est obligatoire")
    @Size(min = 3, max = 255, message = "Le nom doit contenir entre 3 et 255 caractères")
    private String name;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @NotNull(message = "La date de fin estimée est obligatoire")
    @Future(message = "La date de fin estimée doit être dans le futur")
    private LocalDate estimatedEndDate;
}
