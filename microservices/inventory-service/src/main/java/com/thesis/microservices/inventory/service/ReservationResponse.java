package com.thesis.microservices.inventory.service;

import java.util.List;

public record ReservationResponse(
        String status,
        String customerId,
        List<ReservationLineItem> items
) {
}
