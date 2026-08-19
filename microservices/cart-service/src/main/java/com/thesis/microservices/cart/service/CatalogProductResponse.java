package com.thesis.microservices.cart.service;

import java.math.BigDecimal;

public record CatalogProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price
) {
}
