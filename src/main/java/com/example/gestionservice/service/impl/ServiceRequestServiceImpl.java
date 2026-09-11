package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.ServiceRequestCreateRequest;
import com.example.gestionservice.dto.response.PageResponse;
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
import com.example.gestionservice.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final ServiceCatalogRepository catalogRepository;
    private final ServiceRequestMapper mapper;

    @Override
    @Transactional
    public ServiceRequestResponse create(ServiceRequestCreateRequest request, UUID accountId) {
        // accountId provient exclusivement du JWT — jamais du body
        ServiceCatalog catalog = catalogRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCatalog", request.getServiceId()));

        if (!catalog.getActive()) {
            throw new BusinessException("Le service demandé n'est plus actif");
        }

        ServiceRequest entity = ServiceRequest.builder()
                .accountId(accountId)
                .serviceCatalog(catalog)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(catalog.getBasePrice())           // montant depuis le catalogue
                .status(ServiceRequestStatus.WAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        entity = requestRepository.save(entity);
        log.info("Demande créée : id={}, accountId={}, service={}", entity.getId(), accountId, catalog.getName());
        return mapper.toResponse(entity);
    }

    @Override
    public ServiceRequestResponse getById(UUID id, UUID accountId) {
        ServiceRequest entity = requestRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", id));
        return mapper.toResponse(entity);
    }

    @Override
    public PageResponse<ServiceRequestResponse> getMyRequests(UUID accountId, Pageable pageable) {
        return PageResponse.from(
                requestRepository.findByAccountId(accountId, pageable).map(mapper::toResponse));
    }

    @Override
    public PageResponse<ServiceRequestResponse> getAllRequests(Pageable pageable) {
        return PageResponse.from(requestRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional
    public void cancel(UUID id, UUID accountId) {
        ServiceRequest entity = requestRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", id));

        if (entity.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Impossible d'annuler une demande déjà payée");
        }
        if (entity.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new BusinessException("La demande est déjà annulée");
        }
        entity.setStatus(ServiceRequestStatus.CANCELLED);
        entity.setPaymentStatus(PaymentStatus.CANCELLED);
        requestRepository.save(entity);
        log.info("Demande annulée : id={}", id);
    }

    /** Récupère une demande par id en vérifiant qu'elle appartient au compte — ou lève ForbiddenException. */
    public ServiceRequest findAndVerifyOwnership(UUID id, UUID accountId) {
        ServiceRequest entity = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", id));
        if (!entity.getAccountId().equals(accountId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à accéder à cette demande");
        }
        return entity;
    }
}
