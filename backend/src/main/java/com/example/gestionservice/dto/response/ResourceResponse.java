package com.example.gestionservice.dto.response;

import com.example.gestionservice.enums.ResourceAvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UUID specialtyId;
    private String specialtyName;
    private ResourceAvailabilityStatus availabilityStatus;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
