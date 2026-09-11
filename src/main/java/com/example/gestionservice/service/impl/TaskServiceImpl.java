package com.example.gestionservice.service.impl;

import com.example.gestionservice.dto.request.TaskRequest;
import com.example.gestionservice.dto.request.TaskStatusUpdateRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.TaskResponse;
import com.example.gestionservice.entity.Prestation;
import com.example.gestionservice.entity.Resource;
import com.example.gestionservice.entity.Task;
import com.example.gestionservice.enums.TaskStatus;
import com.example.gestionservice.exception.BusinessException;
import com.example.gestionservice.exception.ResourceNotFoundException;
import com.example.gestionservice.mapper.TaskMapper;
import com.example.gestionservice.repository.PrestationRepository;
import com.example.gestionservice.repository.PrestationResourceRepository;
import com.example.gestionservice.repository.ResourceRepository;
import com.example.gestionservice.repository.TaskRepository;
import com.example.gestionservice.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final PrestationRepository prestationRepository;
    private final ResourceRepository resourceRepository;
    private final PrestationResourceRepository prestationResourceRepository;
    private final TaskMapper mapper;

    @Override
    @Transactional
    public TaskResponse create(UUID prestationId, TaskRequest request) {
        Prestation prestation = prestationRepository.findById(prestationId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestation", prestationId));

        // Validation : dueDate > startDate
        if (request.getStartDate() != null && request.getDueDate() != null
                && !request.getDueDate().isAfter(request.getStartDate())) {
            throw new BusinessException("La date d'échéance doit être postérieure à la date de début");
        }

        Task task = mapper.toEntity(request);
        task.setPrestation(prestation);
        task.setStatus(TaskStatus.TODO);
        task = taskRepository.save(task);
        log.info("Tâche créée : id={}, prestationId={}", task.getId(), prestationId);
        return mapper.toResponse(task);
    }

    @Override
    public TaskResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<TaskResponse> getByPrestation(UUID prestationId, Pageable pageable) {
        return PageResponse.from(taskRepository.findByPrestationId(prestationId, pageable).map(mapper::toResponse));
    }

    @Override
    public PageResponse<TaskResponse> getByResource(UUID resourceId, Pageable pageable) {
        return PageResponse.from(taskRepository.findByResourceId(resourceId, pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(UUID id, TaskStatusUpdateRequest request) {
        Task task = findOrThrow(id);
        task.setStatus(request.getStatus());

        // Règle : lorsque status = DONE, completedAt = now
        if (request.getStatus() == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task = taskRepository.save(task);
        log.info("Statut tâche mis à jour : id={}, status={}", id, request.getStatus());
        return mapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse assignResource(UUID taskId, UUID resourceId) {
        Task task = findOrThrow(taskId);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));

        // Règle métier : la ressource doit être affectée à la prestation de la tâche
        UUID prestationId = task.getPrestation().getId();
        boolean isAssigned = prestationResourceRepository.isResourceAssignedToPrestation(prestationId, resourceId);
        if (!isAssigned) {
            throw new BusinessException(
                    "La ressource doit être affectée à la prestation avant de pouvoir lui assigner une tâche");
        }

        task.setResource(resource);
        task = taskRepository.save(task);
        log.info("Tâche affectée : taskId={}, resourceId={}", taskId, resourceId);
        return mapper.toResponse(task);
    }

    private Task findOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
