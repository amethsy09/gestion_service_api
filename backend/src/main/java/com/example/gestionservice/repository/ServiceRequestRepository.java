package com.example.gestionservice.repository;

import com.example.gestionservice.entity.ServiceRequest;
import com.example.gestionservice.enums.PaymentStatus;
import com.example.gestionservice.enums.ServiceRequestStatus;
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
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    /** Toutes les demandes d'un compte client (filtrées par accountId extrait du JWT). */
    Page<ServiceRequest> findByAccountId(UUID accountId, Pageable pageable);

    List<ServiceRequest> findByAccountId(UUID accountId);

    Optional<ServiceRequest> findByIdAndAccountId(UUID id, UUID accountId);

    Page<ServiceRequest> findByStatus(ServiceRequestStatus status, Pageable pageable);

    Page<ServiceRequest> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    /** Toutes les demandes (ADMIN) avec pagination. */
    Page<ServiceRequest> findAll(Pageable pageable);

    /** Vérifie qu'une demande appartient bien à un compte. */
    boolean existsByIdAndAccountId(UUID id, UUID accountId);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.accountId = :accountId AND sr.status != 'CANCELLED'")
    List<ServiceRequest> findActiveByAccountId(@Param("accountId") UUID accountId);
}
