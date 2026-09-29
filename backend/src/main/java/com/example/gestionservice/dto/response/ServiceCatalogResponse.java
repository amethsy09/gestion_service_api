package com.example.gestionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCatalogResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
