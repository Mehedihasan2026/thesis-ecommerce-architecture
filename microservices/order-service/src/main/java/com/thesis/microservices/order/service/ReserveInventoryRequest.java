package com.thesis.microservices.order.service;

import java.util.List;

public record ReserveInventoryRequest(
        String customerId,
        List<ReservationLineItem> items
) {
}
