package com.thesis.microservices.order.service;

public record InventoryResponse(
        Long productId,
        String sku,
        String productName,
        Integer availableQuantity
) {
}
