package com.example.gestionservice.dto.request;

import com.example.gestionservice.enums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequest {

    @NotBlank(message = "Le titre de la tâche est obligatoire")
    @Size(min = 3, max = 255, message = "Le titre doit contenir entre 3 et 255 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    private TaskPriority priority = TaskPriority.MEDIUM;

    @FutureOrPresent(message = "La date de début doit être aujourd'hui ou dans le futur")
    private LocalDate startDate;

    @NotNull(message = "La date d'échéance est obligatoire")
    @FutureOrPresent(message = "La date d'échéance doit être aujourd'hui ou dans le futur")
    private LocalDate dueDate;
}
