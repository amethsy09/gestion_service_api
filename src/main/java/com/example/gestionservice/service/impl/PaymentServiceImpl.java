package com.example.gestionservice.service.impl;

import com.example.gestionservice.client.WalletClient;
import com.example.gestionservice.client.dto.WalletPaymentRequest;
import com.example.gestionservice.client.dto.WalletPaymentResponse;
import com.example.gestionservice.client.dto.WalletPaymentStatusResponse;
import com.example.gestionservice.dto.request.PaymentRequest;
import com.example.gestionservice.dto.response.PaymentResponse;
import com.example.gestionservice.entity.PaymentAttempt;
import com.example.gestionservice.entity.Prestation;
import com.example.gestionservice.entity.ServiceRequest;
import com.example.gestionservice.enums.*;
import com.example.gestionservice.exception.*;
import com.example.gestionservice.repository.PaymentAttemptRepository;
import com.example.gestionservice.repository.PrestationRepository;
import com.example.gestionservice.repository.ServiceRequestRepository;
import com.example.gestionservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de paiement — orchestration complète :
 *  - pay           : initiation paiement + compensation timeout
 *  - retryPayment  : nouvelle tentative après échec
 *  - syncPayment   : synchronisation via idempotencyKey (cas timeout)
 *
 * RÈGLES DE SÉCURITÉ :
 *  - Le PIN n'est jamais stocké, jamais loggé.
 *  - accountId toujours vérifié vs demande (ownership).
 *  - idempotencyKey unique par tentative (UUID.randomUUID).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PrestationRepository prestationRepository;
    private final WalletClient walletClient;

    // ======================================================
    //  PAY — Initiation du paiement
    // ======================================================

    @Override
    @Transactional
    public PaymentResponse pay(UUID serviceRequestId, PaymentRequest request, UUID accountId) {
        ServiceRequest serviceRequest = findAndVerifyRequest(serviceRequestId, accountId);
        validatePayable(serviceRequest);

        // Génération de la clé d'idempotence unique
        UUID idempotencyKey = UUID.randomUUID();
        int attemptNumber = paymentAttemptRepository.countByServiceRequestId(serviceRequestId) + 1;

        // Création de la tentative en statut PROCESSING
        PaymentAttempt attempt = createAttempt(serviceRequest, idempotencyKey, attemptNumber);
        attempt = paymentAttemptRepository.save(attempt);

        log.info("Paiement initié : serviceRequestId={}, attemptNumber={}, idempotencyKey={}",
                serviceRequestId, attemptNumber, idempotencyKey);

        return executePayment(serviceRequest, attempt, request.getPin());
    }

    // ======================================================
    //  RETRY — Nouvelle tentative après échec
    // ======================================================

    @Override
    @Transactional
    public PaymentResponse retryPayment(UUID serviceRequestId, PaymentRequest request, UUID accountId) {
        ServiceRequest serviceRequest = findAndVerifyRequest(serviceRequestId, accountId);

        // Vérifier que le dernier paiement est bien FAILED
        PaymentAttempt lastAttempt = paymentAttemptRepository
                .findLatestByServiceRequestId(serviceRequestId)
                .orElseThrow(() -> new BusinessException("Aucune tentative de paiement trouvée pour cette demande"));

        if (lastAttempt.getStatus() != PaymentAttemptStatus.FAILED) {
            throw new BusinessException(
                    "Le retry n'est possible que si le dernier paiement est en échec (statut actuel : "
                    + lastAttempt.getStatus() + ")");
        }

        // Avant de créer une nouvelle tentative, vérifier via idempotencyKey si
        // le dernier paiement n'a pas été traité côté Wallet (cas timeout)
        checkAndSyncIfProcessing(lastAttempt, serviceRequest);

        // Si après sync la demande est déjà payée, on rejette le retry
        serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", serviceRequestId));
        if (serviceRequest.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentAlreadyProcessedException("Le paiement a déjà été traité avec succès");
        }

        UUID newIdempotencyKey = UUID.randomUUID();
        int attemptNumber = lastAttempt.getAttemptNumber() + 1;

        PaymentAttempt newAttempt = createAttempt(serviceRequest, newIdempotencyKey, attemptNumber);
        newAttempt = paymentAttemptRepository.save(newAttempt);

        log.info("Retry paiement : serviceRequestId={}, attemptNumber={}, idempotencyKey={}",
                serviceRequestId, attemptNumber, newIdempotencyKey);

        return executePayment(serviceRequest, newAttempt, request.getPin());
    }

    // ======================================================
    //  SYNC — Synchronisation en cas de timeout
    // ======================================================

    @Override
    @Transactional
    public PaymentResponse syncPaymentStatus(UUID serviceRequestId, UUID accountId) {
        ServiceRequest serviceRequest = findAndVerifyRequest(serviceRequestId, accountId);

        PaymentAttempt lastAttempt = paymentAttemptRepository
                .findLatestByServiceRequestId(serviceRequestId)
                .orElseThrow(() -> new BusinessException("Aucune tentative de paiement trouvée"));

        if (lastAttempt.getStatus() == PaymentAttemptStatus.SUCCESS) {
            return buildPaymentResponse(serviceRequest, lastAttempt);
        }

        // Interroger le Wallet pour connaître le vrai statut
        WalletPaymentStatusResponse walletStatus = walletClient.getPaymentStatus(lastAttempt.getIdempotencyKey());
        log.info("Sync statut Wallet : idempotencyKey={}, walletStatus={}",
                lastAttempt.getIdempotencyKey(), walletStatus.getStatus());

        return switch (walletStatus.getStatus()) {
            case "SUCCESS" -> {
                applySuccess(serviceRequest, lastAttempt, walletStatus.getTransactionReference());
                yield buildPaymentResponse(serviceRequest, lastAttempt);
            }
            case "FAILED" -> {
                applyFailure(serviceRequest, lastAttempt, walletStatus.getFailureReason());
                yield buildPaymentResponse(serviceRequest, lastAttempt);
            }
            default -> {
                log.info("Sync : paiement toujours en cours (statut Wallet={})", walletStatus.getStatus());
                yield buildPaymentResponse(serviceRequest, lastAttempt);
            }
        };
    }

    // ======================================================
    //  Méthodes privées
    // ======================================================

    private PaymentResponse executePayment(ServiceRequest serviceRequest,
                                           PaymentAttempt attempt,
                                           String pin) {
        try {
            WalletPaymentRequest walletRequest = WalletPaymentRequest.builder()
                    .accountId(serviceRequest.getAccountId())
                    .serviceRequestId(serviceRequest.getId())
                    .amount(serviceRequest.getAmount())
                    .description("Paiement demande : " + serviceRequest.getTitle())
                    .pin(pin)   // transmis, jamais stocké
                    .idempotencyKey(attempt.getIdempotencyKey())
                    .build();

            // Appel Wallet — NE PAS logger walletRequest (contient le PIN)
            WalletPaymentResponse walletResponse = walletClient.pay(walletRequest);

            if ("SUCCESS".equals(walletResponse.getStatus())) {
                applySuccess(serviceRequest, attempt, walletResponse.getTransactionReference());
                log.info("Paiement réussi : serviceRequestId={}, ref={}",
                        serviceRequest.getId(), walletResponse.getTransactionReference());
            } else {
                applyFailure(serviceRequest, attempt, walletResponse.getFailureReason());
                log.warn("Paiement échoué : serviceRequestId={}, raison={}",
                        serviceRequest.getId(), walletResponse.getFailureReason());
            }

        } catch (WalletCommunicationException e) {
            // Timeout ou erreur réseau — on laisse la tentative en PROCESSING pour permettre la sync
            attempt.setStatus(PaymentAttemptStatus.PROCESSING);
            attempt.setFailureReason("Erreur communication Wallet : " + e.getMessage());
            paymentAttemptRepository.save(attempt);
            log.error("Erreur Wallet (timeout probable) : serviceRequestId={}, idempotencyKey={}, error={}",
                    serviceRequest.getId(), attempt.getIdempotencyKey(), e.getMessage());
            throw new WalletCommunicationException(
                    "Le Wallet est temporairement indisponible. Utilisez /payment/sync pour vérifier le statut.", e);
        }

        return buildPaymentResponse(serviceRequest, attempt);
    }

    @Transactional
    protected void applySuccess(ServiceRequest serviceRequest,
                                PaymentAttempt attempt,
                                String transactionReference) {
        attempt.setStatus(PaymentAttemptStatus.SUCCESS);
        attempt.setWalletTransactionReference(transactionReference);
        paymentAttemptRepository.save(attempt);

        serviceRequest.setPaymentStatus(PaymentStatus.PAID);
        serviceRequest.setStatus(ServiceRequestStatus.PAID);
        serviceRequestRepository.save(serviceRequest);

        // Mettre à jour la prestation si elle existe
        Optional<Prestation> prestation = prestationRepository
                .findByServiceRequestId(serviceRequest.getId());
        prestation.ifPresent(p -> {
            p.setStatus(PrestationStatus.PAID);
            prestationRepository.save(p);
        });
    }

    @Transactional
    protected void applyFailure(ServiceRequest serviceRequest,
                                PaymentAttempt attempt,
                                String reason) {
        attempt.setStatus(PaymentAttemptStatus.FAILED);
        attempt.setFailureReason(reason);
        paymentAttemptRepository.save(attempt);

        // La demande reste active — l'utilisateur peut réessayer
        serviceRequest.setPaymentStatus(PaymentStatus.FAILED);
        serviceRequestRepository.save(serviceRequest);
    }

    private void checkAndSyncIfProcessing(PaymentAttempt attempt, ServiceRequest serviceRequest) {
        if (attempt.getStatus() == PaymentAttemptStatus.PROCESSING) {
            try {
                WalletPaymentStatusResponse status = walletClient.getPaymentStatus(attempt.getIdempotencyKey());
                if ("SUCCESS".equals(status.getStatus())) {
                    applySuccess(serviceRequest, attempt, status.getTransactionReference());
                    throw new PaymentAlreadyProcessedException(
                            "Le paiement a été traité avec succès par le Wallet. Pas besoin de retry.");
                }
                if ("FAILED".equals(status.getStatus())) {
                    applyFailure(serviceRequest, attempt, status.getFailureReason());
                }
            } catch (WalletCommunicationException e) {
                log.warn("Impossible de vérifier le statut Wallet avant retry : {}", e.getMessage());
            }
        }
    }

    private PaymentAttempt createAttempt(ServiceRequest serviceRequest,
                                         UUID idempotencyKey,
                                         int attemptNumber) {
        return PaymentAttempt.builder()
                .serviceRequest(serviceRequest)
                .accountId(serviceRequest.getAccountId())
                .amount(serviceRequest.getAmount())
                .status(PaymentAttemptStatus.PROCESSING)
                .idempotencyKey(idempotencyKey)
                .attemptNumber(attemptNumber)
                .build();
    }

    private ServiceRequest findAndVerifyRequest(UUID serviceRequestId, UUID accountId) {
        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", serviceRequestId));

        if (!request.getAccountId().equals(accountId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à payer cette demande");
        }
        return request;
    }

    private void validatePayable(ServiceRequest request) {
        if (request.getPaymentStatus() == PaymentStatus.PAID) {
            throw new PaymentAlreadyProcessedException("Cette demande a déjà été payée");
        }
        if (request.getStatus() == ServiceRequestStatus.CANCELLED) {
            throw new BusinessException("Impossible de payer une demande annulée");
        }
    }

    private PaymentResponse buildPaymentResponse(ServiceRequest serviceRequest,
                                                  PaymentAttempt attempt) {
        return PaymentResponse.builder()
                .serviceRequestId(serviceRequest.getId())
                .serviceRequestPaymentStatus(serviceRequest.getPaymentStatus())
                .paymentAttemptId(attempt.getId())
                .attemptStatus(attempt.getStatus())
                .idempotencyKey(attempt.getIdempotencyKey())
                .walletTransactionReference(attempt.getWalletTransactionReference())
                .amount(attempt.getAmount())
                .attemptNumber(attempt.getAttemptNumber())
                .failureReason(attempt.getFailureReason())
                .processedAt(attempt.getUpdatedAt())
                .build();
    }
}
