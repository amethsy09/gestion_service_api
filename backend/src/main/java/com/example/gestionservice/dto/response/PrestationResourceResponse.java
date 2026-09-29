package com.example.gestionservice.dto.response;

import com.example.gestionservice.enums.ResourceAssignmentStatus;
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
public class PrestationResourceResponse {

    private UUID id;
    private UUID prestationId;
    private UUID resourceId;
    private String resourceFirstName;
    private String resourceLastName;
    private String resourceEmail;
    private String specialtyName;
    private ResourceAssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime unassignedAt;
}
