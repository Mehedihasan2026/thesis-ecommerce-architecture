package com.thesis.microservices.inventory.service;

import java.util.List;

public record ReserveInventoryRequest(
        String customerId,
        List<ReservationLineItem> items
) {
}
