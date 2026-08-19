package com.thesis.microservices.inventory.service;

public record InventoryResponse(
        Long productId,
        String sku,
        String productName,
        Integer availableQuantity
) {
}
