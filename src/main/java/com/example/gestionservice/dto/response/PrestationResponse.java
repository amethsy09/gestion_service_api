package com.example.gestionservice.dto.response;

import com.example.gestionservice.enums.PrestationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrestationResponse {

    private UUID id;
    private UUID serviceRequestId;
    private UUID responsibleId;
    private String responsibleName;
    private String name;
    private String description;
    private PrestationStatus status;
    private LocalDate startDate;
    private LocalDate estimatedEndDate;
    private LocalDate actualEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
