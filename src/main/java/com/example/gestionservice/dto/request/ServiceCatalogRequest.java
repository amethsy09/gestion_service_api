package com.example.gestionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceCatalogRequest {

    @NotBlank(message = "Le nom du service est obligatoire")
    @Size(min = 3, max = 255, message = "Le nom doit contenir entre 3 et 255 caractères")
    private String name;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @NotNull(message = "Le prix de base est obligatoire")
    @Positive(message = "Le prix de base doit être supérieur à 0")
    private BigDecimal basePrice;

    private Boolean active = true;
}
