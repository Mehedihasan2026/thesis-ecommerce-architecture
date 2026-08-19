package com.thesis.microservices.inventory.service;

public record ReservationLineItem(
        Long productId,
        Integer quantity
) {
}
