package com.example.gestionservice.repository;

import com.example.gestionservice.entity.PaymentAttempt;
import com.example.gestionservice.enums.PaymentAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findByServiceRequestIdOrderByAttemptNumberDesc(UUID serviceRequestId);

    Optional<PaymentAttempt> findByIdempotencyKey(UUID idempotencyKey);

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    /** Dernière tentative pour une demande donnée. */
    @Query("""
            SELECT pa FROM PaymentAttempt pa
            WHERE pa.serviceRequest.id = :serviceRequestId
            ORDER BY pa.attemptNumber DESC
            LIMIT 1
            """)
    Optional<PaymentAttempt> findLatestByServiceRequestId(@Param("serviceRequestId") UUID serviceRequestId);

    /** Nombre de tentatives pour une demande. */
    int countByServiceRequestId(UUID serviceRequestId);

    List<PaymentAttempt> findByServiceRequestIdAndStatus(UUID serviceRequestId, PaymentAttemptStatus status);

    /** Tentatives PROCESSING en cours (potentiels timeouts). */
    List<PaymentAttempt> findByStatus(PaymentAttemptStatus status);
}
