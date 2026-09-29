package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.PrestationRequest;
import com.example.gestionservice.dto.response.PrestationResponse;
import com.example.gestionservice.entity.*;
import com.example.gestionservice.enums.PrestationStatus;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.PrestationMapper;
import com.example.gestionservice.mapper.PrestationResourceMapper;
import com.example.gestionservice.repository.*;
import com.example.gestionservice.service.impl.PrestationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrestationService — Tests unitaires")
class PrestationServiceTest {

    @Mock PrestationRepository prestationRepository;
    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock ResponsibleRepository responsibleRepository;
    @Mock ResourceRepository resourceRepository;
    @Mock PrestationResourceRepository prestationResourceRepository;
    @Mock PrestationMapper mapper;
    @Mock PrestationResourceMapper resourceMapper;
    @InjectMocks PrestationServiceImpl service;

    private UUID serviceRequestId;
    private UUID responsibleId;
    private ServiceRequest serviceRequest;
    private Responsible responsible;
    private PrestationRequest request;

    @BeforeEach
    void setUp() {
        serviceRequestId = UUID.randomUUID();
        responsibleId = UUID.randomUUID();

        serviceRequest = ServiceRequest.builder().build();
        responsible = Responsible.builder().active(true).firstName("Jean").lastName("Dupont").build();

        request = new PrestationRequest();
        request.setServiceRequestId(serviceRequestId);
        request.setResponsibleId(responsibleId);
        request.setName("Développement backend");
        request.setDescription("API REST");
        request.setEstimatedEndDate(LocalDate.now().plusMonths(3));
    }

    @Test
    @DisplayName("create — succès")
    void create_success() {
        Prestation saved = Prestation.builder()
                .serviceRequest(serviceRequest)
                .responsible(responsible)
                .name(request.getName())
                .status(PrestationStatus.WAITING_PAYMENT)
                .build();
        PrestationResponse response = PrestationResponse.builder()
                .name("Développement backend")
                .status(PrestationStatus.WAITING_PAYMENT)
                .build();

        when(prestationRepository.existsByServiceRequestId(serviceRequestId)).thenReturn(false);
        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(responsibleRepository.findById(responsibleId)).thenReturn(Optional.of(responsible));
        when(prestationRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        PrestationResponse result = service.create(request);

        assertThat(result.getStatus()).isEqualTo(PrestationStatus.WAITING_PAYMENT);
        assertThat(result.getName()).isEqualTo("Développement backend");
    }

    @Test
    @DisplayName("create — prestation déjà existante pour cette demande → BusinessException")
    void create_duplicateForRequest_throwsBusinessException() {
        when(prestationRepository.existsByServiceRequestId(serviceRequestId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prestation existe déjà");
    }

    @Test
    @DisplayName("start — prestation non payée → BusinessException")
    void start_notPaid_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        Prestation prestation = Prestation.builder().status(PrestationStatus.DRAFT).build();
        when(prestationRepository.findById(id)).thenReturn(Optional.of(prestation));

        assertThatThrownBy(() -> service.start(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("planifiée");
    }

    @Test
    @DisplayName("complete — prestation non en cours → BusinessException")
    void complete_notInProgress_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        Prestation prestation = Prestation.builder().status(PrestationStatus.PLANNED).build();
        when(prestationRepository.findById(id)).thenReturn(Optional.of(prestation));

        assertThatThrownBy(() -> service.complete(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("assignResource — ressource inactive → BusinessException")
    void assignResource_inactiveResource_throwsBusinessException() {
        UUID prestationId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Prestation prestation = Prestation.builder().status(PrestationStatus.PLANNED).build();
        Resource resource = Resource.builder().active(false).build();

        when(prestationRepository.findById(prestationId)).thenReturn(Optional.of(prestation));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.assignResource(prestationId, resourceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }
}
