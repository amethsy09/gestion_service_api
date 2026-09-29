package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.TaskRequest;
import com.example.gestionservice.dto.request.TaskStatusUpdateRequest;
import com.example.gestionservice.dto.response.PageResponse;
import com.example.gestionservice.dto.response.TaskResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {
    TaskResponse create(UUID prestationId, TaskRequest request);
    TaskResponse getById(UUID id);
    PageResponse<TaskResponse> getByPrestation(UUID prestationId, Pageable pageable);
    PageResponse<TaskResponse> getByResource(UUID resourceId, Pageable pageable);
    TaskResponse updateStatus(UUID id, TaskStatusUpdateRequest request);
    TaskResponse assignResource(UUID taskId, UUID resourceId);
}
