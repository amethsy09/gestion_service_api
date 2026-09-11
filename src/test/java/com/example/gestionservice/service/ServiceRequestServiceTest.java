package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.ServiceRequestCreateRequest;
import com.example.gestionservice.dto.response.ServiceRequestResponse;
import com.example.gestionservice.entity.ServiceCatalog;
import com.example.gestionservice.entity.ServiceRequest;
import com.example.gestionservice.enums.PaymentStatus;
import com.example.gestionservice.enums.ServiceRequestStatus;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ForbiddenException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.ServiceRequestMapper;
import com.example.gestionservice.repository.ServiceCatalogRepository;
import com.example.gestionservice.repository.ServiceRequestRepository;
import com.example.gestionservice.service.impl.ServiceRequestServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceRequestService — Tests unitaires")
class ServiceRequestServiceTest {

    @Mock ServiceRequestRepository requestRepository;
    @Mock ServiceCatalogRepository catalogRepository;
    @Mock ServiceRequestMapper mapper;
    @InjectMocks ServiceRequestServiceImpl service;

    private UUID accountId;
    private UUID catalogId;
    private ServiceCatalog catalog;
    private ServiceRequestCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        catalogId = UUID.randomUUID();

        catalog = ServiceCatalog.builder()
                .name("Développement API")
                .basePrice(new BigDecimal("1000000"))
                .active(true)
                .build();

        createRequest = new ServiceRequestCreateRequest();
        createRequest.setServiceId(catalogId);
        createRequest.setTitle("Mon projet API");
        createRequest.setDescription("Besoin d'une API REST");
    }

    @Test
    @DisplayName("create — accountId extrait du JWT, montant depuis le catalogue")
    void create_accountIdFromJwt_amountFromCatalog() {
        ServiceRequest savedEntity = ServiceRequest.builder()
                .accountId(accountId)
                .serviceCatalog(catalog)
                .title(createRequest.getTitle())
                .amount(catalog.getBasePrice())
                .status(ServiceRequestStatus.WAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        ServiceRequestResponse expectedResponse = ServiceRequestResponse.builder()
                .accountId(accountId)
                .serviceName("Développement API")
                .amount(new BigDecimal("1000000"))
                .status(ServiceRequestStatus.WAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(catalog));
        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(savedEntity);
        when(mapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        ServiceRequestResponse result = service.create(createRequest, accountId);

        // Le accountId DOIT provenir du paramètre (JWT), pas du body
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result.getStatus()).isEqualTo(ServiceRequestStatus.WAITING_PAYMENT);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("create — service inactif → BusinessException")
    void create_inactiveService_throwsBusinessException() {
        catalog = ServiceCatalog.builder().name("Old service").active(false).basePrice(BigDecimal.ONE).build();
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(catalog));

        assertThatThrownBy(() -> service.create(createRequest, accountId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("actif");
    }

    @Test
    @DisplayName("getById — utilisateur ne peut pas voir la demande d'un autre")
    void getById_differentAccount_throwsResourceNotFoundException() {
        UUID otherId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findByIdAndAccountId(requestId, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(requestId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancel — demande déjà payée → BusinessException")
    void cancel_alreadyPaid_throwsBusinessException() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest paidRequest = ServiceRequest.builder()
                .accountId(accountId)
                .paymentStatus(PaymentStatus.PAID)
                .status(ServiceRequestStatus.PAID)
                .build();

        when(requestRepository.findByIdAndAccountId(requestId, accountId))
                .thenReturn(Optional.of(paidRequest));

        assertThatThrownBy(() -> service.cancel(requestId, accountId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payée");
    }
}
