package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ServiceCatalogRequest;
import com.example.gestionservice.dto.response.ServiceCatalogResponse;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.ServiceCatalogMapper;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.example.gestionservice.service.impl.ServiceCatalogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCatalogService — Tests unitaires")
class ServiceCatalogServiceTest {

    @Mock ServiceCatalogRepository repository;
    @Mock ServiceCatalogMapper mapper;
    @InjectMocks ServiceCatalogServiceImpl service;

    private ServiceCatalogRequest request;
    private ServiceCatalog entity;
    private ServiceCatalogResponse response;

    @BeforeEach
    void setUp() {
        request = new ServiceCatalogRequest();
        request.setName("Développement API");
        request.setDescription("Création d'une API REST");
        request.setBasePrice(new BigDecimal("1000000"));
        request.setActive(true);

        entity = ServiceCatalog.builder()
                .name("Développement API")
                .description("Création d'une API REST")
                .basePrice(new BigDecimal("1000000"))
                .active(true)
                .build();

        response = ServiceCatalogResponse.builder()
                .id(UUID.randomUUID())
                .name("Développement API")
                .basePrice(new BigDecimal("1000000"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("create — succès")
    void create_success() {
        when(repository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        ServiceCatalogResponse result = service.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Développement API");
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("create — nom déjà existant → BusinessException")
    void create_duplicateName_throwsBusinessException() {
        when(repository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Développement API");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("getById — service introuvable → ResourceNotFoundException")
    void getById_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update — nom en conflit → BusinessException")
    void update_nameConflict_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("delete — suppression réussie")
    void delete_success() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        doNothing().when(repository).delete(entity);

        assertThatCode(() -> service.delete(id)).doesNotThrowAnyException();
        verify(repository).delete(entity);
    }
}
