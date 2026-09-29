package com.example.gestionservice.repository;

import com.example.gestionservice.entity.Task;
import com.example.gestionservice.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByPrestationId(UUID prestationId, Pageable pageable);

    List<Task> findByPrestationId(UUID prestationId);

    Page<Task> findByResourceId(UUID resourceId, Pageable pageable);

    List<Task> findByResourceId(UUID resourceId);

    List<Task> findByPrestationIdAndStatus(UUID prestationId, TaskStatus status);

    /** Tâches en retard (dueDate dépassée et non terminées). */
    @Query("""
            SELECT t FROM Task t
            WHERE t.dueDate < :today
              AND t.status NOT IN ('DONE', 'CANCELLED')
            """)
    List<Task> findOverdueTasks(@Param("today") LocalDate today);

    @Query("""
            SELECT t FROM Task t
            WHERE t.prestation.id = :prestationId
              AND t.resource.id   = :resourceId
            """)
    List<Task> findByPrestationIdAndResourceId(
            @Param("prestationId") UUID prestationId,
            @Param("resourceId") UUID resourceId);
}
