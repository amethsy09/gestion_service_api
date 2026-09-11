package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.TaskRequest;
import com.example.gestionservice.dto.request.TaskStatusUpdateRequest;
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
import com.example.gestionservice.service.impl.TaskServiceImpl;
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
@DisplayName("TaskService — Tests unitaires")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock PrestationRepository prestationRepository;
    @Mock ResourceRepository resourceRepository;
    @Mock PrestationResourceRepository prestationResourceRepository;
    @Mock TaskMapper mapper;
    @InjectMocks TaskServiceImpl service;

    private UUID prestationId;
    private Prestation prestation;
    private TaskRequest request;

    @BeforeEach
    void setUp() {
        prestationId = UUID.randomUUID();
        prestation = Prestation.builder().build();

        request = new TaskRequest();
        request.setTitle("Développer le module auth");
        request.setStartDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(10));
    }

    @Test
    @DisplayName("create — succès avec statut TODO par défaut")
    void create_success_defaultStatusTodo() {
        Task savedTask = Task.builder()
                .prestation(prestation)
                .title(request.getTitle())
                .status(TaskStatus.TODO)
                .build();
        TaskResponse response = TaskResponse.builder()
                .title("Développer le module auth")
                .status(TaskStatus.TODO)
                .build();

        when(prestationRepository.findById(prestationId)).thenReturn(Optional.of(prestation));
        when(mapper.toEntity(request)).thenReturn(savedTask);
        when(taskRepository.save(any())).thenReturn(savedTask);
        when(mapper.toResponse(savedTask)).thenReturn(response);

        TaskResponse result = service.create(prestationId, request);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    @DisplayName("create — dueDate avant startDate → BusinessException")
    void create_dueDateBeforeStartDate_throwsBusinessException() {
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setDueDate(LocalDate.now());                // dueDate < startDate

        when(prestationRepository.findById(prestationId)).thenReturn(Optional.of(prestation));

        assertThatThrownBy(() -> service.create(prestationId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("échéance");
    }

    @Test
    @DisplayName("updateStatus — DONE → completedAt automatique")
    void updateStatus_done_completedAtSet() {
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder().status(TaskStatus.IN_PROGRESS).build();
        TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest();
        statusRequest.setStatus(TaskStatus.DONE);
        TaskResponse response = TaskResponse.builder().status(TaskStatus.DONE).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(response);

        service.updateStatus(taskId, statusRequest);

        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("assignResource — ressource non affectée à la prestation → BusinessException")
    void assignResource_resourceNotInPrestation_throwsBusinessException() {
        UUID taskId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Task task = Task.builder().prestation(prestation).build();
        Resource resource = Resource.builder().build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(prestationResourceRepository.isResourceAssignedToPrestation(any(), eq(resourceId)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.assignResource(taskId, resourceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("affectée à la prestation");
    }
}
