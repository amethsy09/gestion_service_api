package com.example.gestionservice.service;

import com.example.gestionservice.dto.request.PaymentRequest;
import com.example.gestionservice.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse pay(UUID serviceRequestId, PaymentRequest request, UUID accountId);
    PaymentResponse retryPayment(UUID serviceRequestId, PaymentRequest request, UUID accountId);
    PaymentResponse syncPaymentStatus(UUID serviceRequestId, UUID accountId);
}
