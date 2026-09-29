package com.example.gestionservice.dto.response;

import com.example.gestionservice.enums.PaymentStatus;
import com.example.gestionservice.enums.ServiceRequestStatus;
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
public class ServiceRequestResponse {

    private UUID id;
    private UUID accountId;
    private UUID serviceCatalogId;
    private String serviceName;
    private String title;
    private String description;
    private BigDecimal amount;
    private ServiceRequestStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
