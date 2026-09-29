package com.example.gestionservice.service;

import com.example.gestionservice.client.WalletClient;
import com.example.gestionservice.client.dto.WalletPaymentResponse;
import com.example.gestionservice.client.dto.WalletPaymentStatusResponse;
import com.example.gestionservice.dto.request.PaymentRequest;
import com.example.gestionservice.dto.response.PaymentResponse;
import com.example.gestionservice.entity.PaymentAttempt;
import com.example.gestionservice.entity.Prestation;
import com.example.gestionservice.entity.ServiceRequest;
import com.example.gestionservice.enums.*;
import com.example.gestionservice.exception.*;
import com.example.gestionservice.repository.GestionAccountRepository;
import com.example.gestionservice.repository.PaymentAttemptRepository;
import com.example.gestionservice.repository.PrestationRepository;
import com.example.gestionservice.repository.ServiceRequestRepository;
import com.example.gestionservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — Tests unitaires")
class PaymentServiceTest {

    @Mock ServiceRequestRepository serviceRequestRepository;
    @Mock PaymentAttemptRepository paymentAttemptRepository;
    @Mock PrestationRepository prestationRepository;
    @Mock WalletClient walletClient;
    @Mock GestionAccountRepository gestionAccountRepository;
    @InjectMocks PaymentServiceImpl service;

    private UUID accountId;
    private String telephone;
    private UUID serviceRequestId;
    private ServiceRequest serviceRequest;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        telephone = "771234567";
        serviceRequestId = UUID.randomUUID();

        serviceRequest = ServiceRequest.builder()
                .id(serviceRequestId)
                .accountId(accountId)
                .amount(new BigDecimal("1000000"))
                .status(ServiceRequestStatus.WAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .title("Mon projet")
                .build();

        paymentRequest = new PaymentRequest();
        paymentRequest.setPin("1234");
    }

    @Test
    @DisplayName("pay — succès Wallet → ServiceRequest PAID + Prestation PAID")
    void pay_walletSuccess_updatesRequestAndPrestation() {
        Prestation prestation = Prestation.builder()
                .serviceRequest(serviceRequest)
                .status(PrestationStatus.WAITING_PAYMENT)
                .build();

        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(paymentAttemptRepository.countByServiceRequestId(serviceRequestId)).thenReturn(0);
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.pay(any())).thenReturn(
                WalletPaymentResponse.builder().status("SUCCESS").transactionReference("TXN-001").build());
        when(prestationRepository.findByServiceRequestId(serviceRequestId)).thenReturn(Optional.of(prestation));
        when(serviceRequestRepository.save(any())).thenReturn(serviceRequest);
        when(prestationRepository.save(any())).thenReturn(prestation);

        PaymentResponse result = service.pay(serviceRequestId, paymentRequest, accountId, telephone);

        assertThat(result.getAttemptStatus()).isEqualTo(PaymentAttemptStatus.SUCCESS);
        assertThat(serviceRequest.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(prestation.getStatus()).isEqualTo(PrestationStatus.PAID);
        // Le PIN ne doit jamais apparaître dans la réponse
        assertThat(result.toString()).doesNotContain("1234");
    }

    @Test
    @DisplayName("pay — échec Wallet → FAILED, demande non supprimée, retry possible")
    void pay_walletFailed_requestRemainsRetryAllowed() {
        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(paymentAttemptRepository.countByServiceRequestId(serviceRequestId)).thenReturn(0);
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.pay(any())).thenReturn(
                WalletPaymentResponse.builder().status("FAILED").failureReason("Solde insuffisant").build());
        when(serviceRequestRepository.save(any())).thenReturn(serviceRequest);

        PaymentResponse result = service.pay(serviceRequestId, paymentRequest, accountId, telephone);

        assertThat(result.getAttemptStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(serviceRequest.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(serviceRequestRepository, never()).delete(any());
    }

    @Test
    @DisplayName("pay — demande déjà payée → PaymentAlreadyProcessedException")
    void pay_alreadyPaid_throws() {
        serviceRequest = ServiceRequest.builder()
                .accountId(accountId)
                .paymentStatus(PaymentStatus.PAID)
                .status(ServiceRequestStatus.PAID)
                .amount(BigDecimal.ONE)
                .build();
        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));

        assertThatThrownBy(() -> service.pay(serviceRequestId, paymentRequest, accountId, telephone))
                .isInstanceOf(PaymentAlreadyProcessedException.class);
        verify(walletClient, never()).pay(any());
    }

    @Test
    @DisplayName("pay — mauvais compte → ForbiddenException")
    void pay_wrongAccount_throws() {
        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));

        assertThatThrownBy(() -> service.pay(serviceRequestId, paymentRequest, UUID.randomUUID(), "770000000"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("retryPayment — dernière tentative non FAILED → BusinessException")
    void retryPayment_lastAttemptNotFailed_throws() {
        PaymentAttempt attempt = PaymentAttempt.builder()
                .status(PaymentAttemptStatus.SUCCESS)
                .idempotencyKey(UUID.randomUUID())
                .attemptNumber(1)
                .build();

        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(paymentAttemptRepository.findLatestByServiceRequestId(serviceRequestId))
                .thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.retryPayment(serviceRequestId, paymentRequest, accountId, telephone))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("retry");
    }

    @Test
    @DisplayName("retryPayment — retry après échec → nouvel idempotencyKey, attemptNumber incrémenté")
    void retryPayment_afterFailure_newIdempotencyKey() {
        UUID oldKey = UUID.randomUUID();
        PaymentAttempt failedAttempt = PaymentAttempt.builder()
                .status(PaymentAttemptStatus.FAILED)
                .idempotencyKey(oldKey)
                .attemptNumber(1)
                .build();

        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(paymentAttemptRepository.findLatestByServiceRequestId(serviceRequestId))
                .thenReturn(Optional.of(failedAttempt));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> {
            PaymentAttempt saved = inv.getArgument(0);
            assertThat(saved.getIdempotencyKey()).isNotEqualTo(oldKey);
            assertThat(saved.getAttemptNumber()).isEqualTo(2);
            return saved;
        });
        when(walletClient.pay(any())).thenReturn(
                WalletPaymentResponse.builder().status("SUCCESS").transactionReference("TXN-002").build());
        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(prestationRepository.findByServiceRequestId(serviceRequestId)).thenReturn(Optional.empty());
        when(serviceRequestRepository.save(any())).thenReturn(serviceRequest);

        PaymentResponse result = service.retryPayment(serviceRequestId, paymentRequest, accountId, telephone);

        assertThat(result.getAttemptStatus()).isEqualTo(PaymentAttemptStatus.SUCCESS);
        assertThat(result.getAttemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("syncPaymentStatus — Wallet SUCCESS → met à jour la demande")
    void syncPaymentStatus_walletSuccess_updatesRequest() {
        UUID idempotencyKey = UUID.randomUUID();
        PaymentAttempt attempt = PaymentAttempt.builder()
                .status(PaymentAttemptStatus.PROCESSING)
                .idempotencyKey(idempotencyKey)
                .attemptNumber(1)
                .amount(new BigDecimal("1000000"))
                .build();

        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(paymentAttemptRepository.findLatestByServiceRequestId(serviceRequestId))
                .thenReturn(Optional.of(attempt));
        when(walletClient.getPaymentStatus(idempotencyKey)).thenReturn(
                WalletPaymentStatusResponse.builder().status("SUCCESS").transactionReference("TXN-SYNC").build());
        when(paymentAttemptRepository.save(any())).thenReturn(attempt);
        when(serviceRequestRepository.save(any())).thenReturn(serviceRequest);
        when(prestationRepository.findByServiceRequestId(serviceRequestId)).thenReturn(Optional.empty());

        PaymentResponse result = service.syncPaymentStatus(serviceRequestId, accountId);

        assertThat(result.getAttemptStatus()).isEqualTo(PaymentAttemptStatus.SUCCESS);
        assertThat(attempt.getWalletTransactionReference()).isEqualTo("TXN-SYNC");
        assertThat(serviceRequest.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("timeout — WalletCommunicationException → tentative laissée en PROCESSING")
    void pay_timeout_attemptLeftProcessing() {
        when(serviceRequestRepository.findById(serviceRequestId)).thenReturn(Optional.of(serviceRequest));
        when(paymentAttemptRepository.countByServiceRequestId(serviceRequestId)).thenReturn(0);
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.pay(any())).thenThrow(
                new WalletCommunicationException("Le Wallet est temporairement indisponible"));

        assertThatThrownBy(() -> service.pay(serviceRequestId, paymentRequest, accountId, telephone))
                .isInstanceOf(WalletCommunicationException.class)
                .hasMessageContaining("sync");

        assertThat(serviceRequest.getPaymentStatus()).isNotEqualTo(PaymentStatus.PAID);
    }
}
