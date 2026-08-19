package com.thesis.microservices.cart.service;

public record AddCartItemRequest(
        Long productId,
        Integer quantity
) {
}
