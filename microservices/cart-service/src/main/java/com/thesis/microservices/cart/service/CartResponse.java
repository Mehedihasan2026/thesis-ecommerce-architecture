package com.thesis.microservices.cart.service;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
}
