package com.thesis.monolith.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String customerId,
        String status,
        BigDecimal totalAmount,
        Instant createdAt,
        Long paymentTransactionId,
        String paymentStatus,
        List<OrderItemResponse> items
) {
}
