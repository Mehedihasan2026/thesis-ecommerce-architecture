package com.thesis.microservices.payment.service;

import java.math.BigDecimal;

public record PaymentRequest(
        String customerId,
        BigDecimal amount,
        boolean simulatePaymentFailure,
        long artificialPaymentDelayMs
) {
}
