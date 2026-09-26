package com.example.gestionservice.entity;

import com.example.gestionservice.enums.TaskPriority;
import com.example.gestionservice.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tâche appartenant à une prestation, pouvant être affectée à une ressource.
 * La ressource affectée doit appartenir à la même prestation.
 */
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_prestation_id", columnList = "prestation_id"),
        @Index(name = "idx_tasks_resource_id", columnList = "resource_id"),
        @Index(name = "idx_tasks_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
public class Task extends BaseEntity {

    protected Task() {
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestation_id", nullable = false)
    private Prestation prestation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
