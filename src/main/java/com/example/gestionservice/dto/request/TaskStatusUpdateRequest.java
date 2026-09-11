package com.example.gestionservice.dto.request;

import com.example.gestionservice.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateRequest {

    @NotNull(message = "Le statut est obligatoire")
    private TaskStatus status;
}
