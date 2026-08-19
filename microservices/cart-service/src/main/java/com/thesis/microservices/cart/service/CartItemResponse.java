package com.thesis.microservices.cart.service;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        java.math.BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
}
