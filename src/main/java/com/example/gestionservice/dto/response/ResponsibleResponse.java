package com.example.gestionservice.dto.response;

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
public class ResponsibleResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
