package com.thesis.microservices.order.service;

public record ReservationLineItem(
        Long productId,
        Integer quantity
) {
}
