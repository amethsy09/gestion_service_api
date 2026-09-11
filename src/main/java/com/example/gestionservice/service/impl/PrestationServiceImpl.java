package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.PrestationRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.PrestationResourceResponse;
import com.example.gestionservice.dto.response.PrestationResponse;
import com.example.gestionservice.entity.*;
import com.example.gestionservice.enums.PrestationStatus;
import com.example.gestionservice.enums.ResourceAssignmentStatus;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.PrestationMapper;
import com.example.gestionservice.mapper.PrestationResourceMapper;
import com.example.gestionservice.repository.*;
import com.example.gestionservice.service.PrestationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrestationServiceImpl implements PrestationService {

    private final PrestationRepository prestationRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ResponsibleRepository responsibleRepository;
    private final ResourceRepository resourceRepository;
    private final PrestationResourceRepository prestationResourceRepository;
    private final PrestationMapper mapper;
    private final PrestationResourceMapper resourceMapper;

    @Override
    @Transactional
    public PrestationResponse create(PrestationRequest request) {
        // Vérifier unicité : une demande ne peut avoir qu'une seule prestation
        if (prestationRepository.existsByServiceRequestId(request.getServiceRequestId())) {
            throw new BusinessException("Une prestation existe déjà pour cette demande de service");
        }

        ServiceRequest serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", request.getServiceRequestId()));

        Responsible responsible = responsibleRepository.findById(request.getResponsibleId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsible", request.getResponsibleId()));

        if (!responsible.getActive()) {
            throw new BusinessException("Le responsable sélectionné n'est plus actif");
        }

        Prestation entity = Prestation.builder()
                .serviceRequest(serviceRequest)
                .responsible(responsible)
                .name(request.getName())
                .description(request.getDescription())
                .estimatedEndDate(request.getEstimatedEndDate())
                .status(PrestationStatus.WAITING_PAYMENT)
                .build();

        entity = prestationRepository.save(entity);
        log.info("Prestation créée : id={}, serviceRequestId={}", entity.getId(), request.getServiceRequestId());
        return mapper.toResponse(entity);
    }

    @Override
    public PrestationResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<PrestationResponse> getAll(Pageable pageable) {
        return PageResponse.from(prestationRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional
    public PrestationResponse plan(UUID id) {
        Prestation entity = findOrThrow(id);
        if (entity.getStatus() != PrestationStatus.PAID) {
            throw new BusinessException("La prestation doit être payée avant d'être planifiée (statut actuel : " + entity.getStatus() + ")");
        }
        entity.setStatus(PrestationStatus.PLANNED);
        entity = prestationRepository.save(entity);
        log.info("Prestation planifiée : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PrestationResponse start(UUID id) {
        Prestation entity = findOrThrow(id);
        // Règle métier : une prestation ne peut pas démarrer avant paiement
        if (entity.getStatus() != PrestationStatus.PLANNED && entity.getStatus() != PrestationStatus.PAID) {
            throw new BusinessException("La prestation doit être planifiée ou payée avant de démarrer (statut actuel : " + entity.getStatus() + ")");
        }
        entity.setStatus(PrestationStatus.IN_PROGRESS);
        entity = prestationRepository.save(entity);
        log.info("Prestation démarrée : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PrestationResponse complete(UUID id) {
        Prestation entity = findOrThrow(id);
        if (entity.getStatus() != PrestationStatus.IN_PROGRESS) {
            throw new BusinessException("La prestation doit être en cours pour être terminée (statut actuel : " + entity.getStatus() + ")");
        }
        entity.setStatus(PrestationStatus.COMPLETED);
        entity = prestationRepository.save(entity);
        log.info("Prestation terminée : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PrestationResponse cancel(UUID id) {
        Prestation entity = findOrThrow(id);
        if (entity.getStatus() == PrestationStatus.COMPLETED) {
            throw new BusinessException("Impossible d'annuler une prestation terminée");
        }
        entity.setStatus(PrestationStatus.CANCELLED);
        entity = prestationRepository.save(entity);
        log.info("Prestation annulée : id={}", id);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PrestationResourceResponse assignResource(UUID prestationId, UUID resourceId) {
        Prestation prestation = findOrThrow(prestationId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));

        if (!resource.getActive()) {
            throw new BusinessException("Impossible d'affecter une ressource inactive");
        }
        if (prestationResourceRepository.existsByPrestationIdAndResourceId(prestationId, resourceId)) {
            throw new BusinessException("Cette ressource est déjà affectée à la prestation");
        }

        PrestationResource assignment = PrestationResource.builder()
                .prestation(prestation)
                .resource(resource)
                .assignedAt(LocalDateTime.now())
                .status(ResourceAssignmentStatus.ASSIGNED)
                .build();

        assignment = prestationResourceRepository.save(assignment);
        log.info("Ressource affectée : prestationId={}, resourceId={}", prestationId, resourceId);
        return resourceMapper.toResponse(assignment);
    }

    @Override
    @Transactional
    public void removeResource(UUID prestationId, UUID resourceId) {
        PrestationResource assignment = prestationResourceRepository
                .findByPrestationIdAndResourceId(prestationId, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Affectation introuvable pour prestationId=" + prestationId + " et resourceId=" + resourceId));

        assignment.setStatus(ResourceAssignmentStatus.REMOVED);
        assignment.setUnassignedAt(LocalDateTime.now());
        prestationResourceRepository.save(assignment);
        log.info("Ressource retirée : prestationId={}, resourceId={}", prestationId, resourceId);
    }

    @Override
    public List<PrestationResourceResponse> getResources(UUID prestationId) {
        findOrThrow(prestationId); // vérification existence
        return prestationResourceRepository.findByPrestationId(prestationId).stream()
                .map(resourceMapper::toResponse)
                .toList();
    }

    private Prestation findOrThrow(UUID id) {
        return prestationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestation", id));
    }
}
