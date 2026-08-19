package com.thesis.microservices.order.service;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long transactionId,
        String customerId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
}
