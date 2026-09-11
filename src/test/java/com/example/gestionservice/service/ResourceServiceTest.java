package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ResourceRequest;
import com.example.gestionservice.dto.response.ResourceResponse;
import com.example.gestionservice.entity.Resource;
import com.example.gestionservice.entity.Specialty;
import com.example.gestionservice.enums.ResourceAvailabilityStatus;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.mapper.ResourceMapper;
import com.example.gestionservice.repository.ResourceRepository;
import com.example.gestionservice.repository.SpecialtyRepository;
import com.example.gestionservice.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceService — Tests unitaires")
class ResourceServiceTest {

    @Mock ResourceRepository repository;
    @Mock SpecialtyRepository specialtyRepository;
    @Mock ResourceMapper mapper;
    @InjectMocks ResourceServiceImpl service;

    private ResourceRequest request;
    private Specialty specialty;

    @BeforeEach
    void setUp() {
        UUID specialtyId = UUID.randomUUID();
        specialty = Specialty.builder().name("Backend Developer").build();

        request = new ResourceRequest();
        request.setFirstName("Alice");
        request.setLastName("Martin");
        request.setEmail("alice@example.com");
        request.setSpecialtyId(specialtyId);
        request.setAvailabilityStatus(ResourceAvailabilityStatus.AVAILABLE);
        request.setActive(true);
    }

    @Test
    @DisplayName("create — succès avec spécialité résolue")
    void create_success() {
        Resource entity = Resource.builder().firstName("Alice").email("alice@example.com").build();
        ResourceResponse response = ResourceResponse.builder().firstName("Alice").specialtyName("Backend Developer").build();

        when(repository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(specialtyRepository.findById(request.getSpecialtyId())).thenReturn(Optional.of(specialty));
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        ResourceResponse result = service.create(request);

        assertThat(result.getFirstName()).isEqualTo("Alice");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("create — email déjà utilisé → BusinessException")
    void create_duplicateEmail_throws() {
        when(repository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("alice@example.com");
        verify(repository, never()).save(any());
    }
}
