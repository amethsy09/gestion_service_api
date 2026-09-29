package com.example.gestionservice.repository;

import com.example.gestionservice.entity.Prestation;
import com.example.gestionservice.enums.PrestationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrestationRepository extends JpaRepository<Prestation, UUID> {

    /** Vérifie qu'une demande n'a pas déjà une prestation (unicité 1:1). */
    boolean existsByServiceRequestId(UUID serviceRequestId);

    Optional<Prestation> findByServiceRequestId(UUID serviceRequestId);

    List<Prestation> findByResponsibleId(UUID responsibleId);

    Page<Prestation> findByStatus(PrestationStatus status, Pageable pageable);

    Page<Prestation> findAll(Pageable pageable);

    @Query("SELECT p FROM Prestation p WHERE p.responsible.id = :responsibleId AND p.status NOT IN ('COMPLETED','CANCELLED')")
    List<Prestation> findActiveByResponsibleId(@Param("responsibleId") UUID responsibleId);
}
